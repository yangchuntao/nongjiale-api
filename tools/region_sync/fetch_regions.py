"""全国省市区三级行政区划数据抓取与归一。

数据源：DataV.GeoAtlas 行政区划边界服务（https://geo.datav.aliyun.com/areas_v3/bound/）。
只取 properties 属性，不下载 geometry，避免把几十 MB 边界数据塞进库里。

用法：
    python tools/region_sync/fetch_regions.py                # 抓全国，输出 data/regions.csv
    python tools/region_sync/fetch_regions.py --province 320000  # 只抓江苏

产出 CSV 列：adcode,name,level,parent_adcode,sort_order,center_lng,center_lat
level 取 1 省 / 2 市 / 3 区县，与 RegionLevel 枚举 code 对齐。
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = "https://geo.datav.aliyun.com/areas_v3/bound"
TIMEOUT = 30
# 数据源偶发 5xx，重试两次再放弃，避免一次抖动丢掉整条分支
RETRIES = 3
RETRY_DELAY = 2.0

# 省级根节点。用 100000_full 一次拿到 34 个省级单元（含港澳台），比逐个请求省事。
COUNTRY_FULL = f"{BASE}/100000_full.json"

# 直辖市与港澳特别行政区在国家数据源里是「省 → 区县」两层，缺少市这一级。
# 本平台的开城单位是市，缺市会导致这些地方永远无法开通，这里补一个合成市级节点：
# 键为省级 adcode，值为 (市级 adcode, 市级名称, 短名)。市级 adcode 采用省级代码 + 01 的约定，
# 不与任何真实区县代码冲突（真实区县从 0101 起，如北京东城 110101）。
SYNTHETIC_CITIES = {
    "110000": ("110100", "北京市", "北京"),
    "120000": ("120100", "天津市", "天津"),
    "310000": ("310100", "上海市", "上海"),
    "500000": ("500100", "重庆市", "重庆"),
    "810000": ("810100", "香港特别行政区", "香港"),
    "820000": ("820100", "澳门特别行政区", "澳门"),
}


def synthetic_city(province: dict, sort_order: int) -> dict | None:
    """为直辖市/港澳补一个合成市级节点；其他省返回 None。"""
    adcode = str(province["adcode"])
    spec = SYNTHETIC_CITIES.get(adcode)
    if spec is None:
        return None
    city_adcode, name, short = spec
    return {
        "adcode": city_adcode,
        "name": name,
        "level": 2,
        "parent_adcode": adcode,
        "sort_order": sort_order,
        "center_lng": "",
        "center_lat": "",
    }



def fetch_json(url: str) -> dict | None:
    """拉一个 JSON；404 返回 None（表示该节点无下级），其他错误重试后抛出。"""
    last_error: Exception | None = None
    for attempt in range(RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "nongjiale-region-sync/1.0"})
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                return None
            last_error = exc
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            last_error = exc
        if attempt < RETRIES - 1:
            time.sleep(RETRY_DELAY)
    print(f"  ! 失败 {url}: {last_error}", file=sys.stderr)
    return None


def features(payload: dict | None) -> list[dict]:
    """从 FeatureCollection 里取 properties；跳过没有 level 的占位要素（如 100000_JD 九段线）。"""
    if not payload:
        return []
    rows = []
    for feature in payload.get("features", []):
        props = feature.get("properties") or {}
        if not props.get("level") or not props.get("adcode"):
            continue
        rows.append(props)
    return rows


def level_code(level: str) -> int | None:
    """数据源 level 字符串映射到 RegionLevel 的数字编码。"""
    return {"province": 1, "city": 2, "district": 3}.get(level)


def center_of(props: dict) -> tuple[str, str]:
    """取政府驻地经纬度；缺失时留空，不用质心冒充驻地。"""
    center = props.get("center") or []
    if len(center) == 2:
        return f"{center[0]:.6f}", f"{center[1]:.6f}"
    return "", ""


def collect(province_filter: str | None = None) -> list[dict]:
    """遍历省 → 市 → 区县，返回扁平记录。父级一律取 acroutes 的上一级，不靠名称猜。"""
    print("拉取省级清单…")
    provinces = features(fetch_json(COUNTRY_FULL))
    print(f"  省级 {len(provinces)} 个（含港澳台）")

    records: list[dict] = []

    for index, province in enumerate(provinces, start=1):
        adcode = str(province["adcode"])
        if province_filter and adcode != province_filter:
            continue
        name = province.get("name") or ""
        print(f"[{index}/{len(provinces)}] {name} {adcode}")
        records.append(build(province, parent=None, sort_order=index * 100))

        # 直辖市/港澳：补合成市级节点，并把下辖区县改挂到它下面
        synthetic = synthetic_city(province, sort_order=100)
        if synthetic is not None:
            records.append(synthetic)
            direct = features(fetch_json(f"{BASE}/{adcode}_full.json"))
            print(f"    直辖市/港澳结构，合成市级节点 {synthetic['name']}，下辖区县 {len(direct)} 个")
            for district_index, district in enumerate(direct, start=1):
                records.append(build(district, parent=synthetic["adcode"], sort_order=district_index * 100))
            continue

        cities = features(fetch_json(f"{BASE}/{adcode}_full.json"))
        print(f"    下辖市 {len(cities)} 个")
        for city_index, city in enumerate(cities, start=1):
            records.append(build(city, parent=adcode, sort_order=city_index * 100))

            city_adcode = str(city["adcode"])
            # 直辖市的“市”下面直接是区，层级正常；县级市下面 childrenNum=0，请求会 404
            districts = features(fetch_json(f"{BASE}/{city_adcode}_full.json"))
            if districts:
                print(f"      {city.get('name')} 下辖区县 {len(districts)} 个")
            for district_index, district in enumerate(districts, start=1):
                records.append(build(district, parent=city_adcode, sort_order=district_index * 100))
        time.sleep(0.15)

    return records


def build(props: dict, parent: str | None, sort_order: int) -> dict:
    """把数据源 properties 归一成一行 region 记录。"""
    adcode = str(props["adcode"])
    lng, lat = center_of(props)
    return {
        "adcode": adcode,
        "name": props.get("name") or "",
        "level": level_code(props.get("level") or ""),
        "parent_adcode": parent or "",
        "sort_order": sort_order,
        "center_lng": lng,
        "center_lat": lat,
    }


def validate(records: list[dict]) -> list[str]:
    """校验：编码唯一、层级合法、父级存在、父子层级相邻。返回问题清单，空表示通过。"""
    problems: list[str] = []
    seen: dict[str, dict] = {}
    for row in records:
        adcode = row["adcode"]
        if adcode in seen:
            problems.append(f"编码重复: {adcode} {row['name']}")
        seen[adcode] = row
        if row["level"] not in (1, 2, 3):
            problems.append(f"层级非法: {adcode} {row['name']} level={row['level']}")
        if not row["name"]:
            problems.append(f"名称为空: {adcode}")

    for row in records:
        parent = row["parent_adcode"]
        if not parent:
            if row["level"] != 1:
                problems.append(f"缺父级: {row['adcode']} {row['name']}")
            continue
        parent_row = seen.get(parent)
        if parent_row is None:
            problems.append(f"父级不存在: {row['adcode']} {row['name']} -> {parent}")
        elif parent_row["level"] != row["level"] - 1:
            problems.append(
                f"层级不连续: {row['adcode']} {row['name']}({row['level']}) 父级 {parent}({parent_row['level']})"
            )
    return problems


def write_csv(records: list[dict], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = ["adcode", "name", "level", "parent_adcode", "sort_order", "center_lng", "center_lat"]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(records)


def main() -> int:
    parser = argparse.ArgumentParser(description="抓取全国省市区三级行政区划属性数据")
    parser.add_argument("--province", help="只抓某个省，传省级 adcode，如 320000")
    parser.add_argument("--out", default="data/regions.csv", help="输出 CSV 路径")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent
    out = Path(args.out) if Path(args.out).is_absolute() else root / args.out

    records = collect(args.province)
    problems = validate(records)

    print()
    print(f"共 {len(records)} 条：省 {sum(1 for r in records if r['level'] == 1)} / "
          f"市 {sum(1 for r in records if r['level'] == 2)} / "
          f"区县 {sum(1 for r in records if r['level'] == 3)}")
    if problems:
        print(f"校验问题 {len(problems)} 条：", file=sys.stderr)
        for problem in problems[:20]:
            print(f"  - {problem}", file=sys.stderr)
        if len(problems) > 20:
            print(f"  … 其余 {len(problems) - 20} 条略", file=sys.stderr)
    else:
        print("校验通过：编码唯一、层级连续、父级齐全")

    write_csv(records, out)
    print(f"已写出 {out}")
    return 0 if not problems else 1


if __name__ == "__main__":
    raise SystemExit(main())
