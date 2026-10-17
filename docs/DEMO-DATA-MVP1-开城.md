# 开城演示数据扩充 · MVP-1（无密码）

| 项 | 内容 |
|----|------|
| 日期 | 2026-10-17 约 23:58 Asia/Shanghai |
| 测服 API | `b25e3d6` + Flyway V4 |
| 方式 | 运营代建 `POST /api/admin/merchants/{orgId}/stores`；`testData=false` 计入 N |
| 账号 | 沿用 S5 三端种子；密码见 `private/DEMO-ACCOUNTS-S5.md`（勿贴群） |

## 城市清单（造数后抽检）

| 市 | cityId | openStatus | storeCount（ON_SHELF∧¬test_data） | articleCount | 用途 |
|----|--------|------------|-----------------------------------|--------------|------|
| 扬州 | 2 | OPEN | **5** | **1** | 首开达标可开（N=5/M=1） |
| 泰州 | 3 | OPEN | 1 | 1 | 供给仍低于 N |
| 南通 | 4 | CLOSED* | 1 | 0 | **故意低于 N/M**，测未达标禁开（*EXEC 跑测可能已关，保持） |

## 新增扬州门店（计入 N）

| id | 名称 | region | shelf | testData | 推荐 |
|----|------|--------|-------|----------|------|
| 11 | 扬州·瘦西湖畔小院 | 广陵 5 | ON_SHELF | false | 是 |
| 12 | 广陵·东关街石巷宿 | 广陵 5 | ON_SHELF | false | 是 |
| 13 | 广陵·运河夜色民宿 | 广陵 5 | ON_SHELF | false | 否 |
| 14 | 扬州·茱萸湾观景楼 | 广陵 5 | ON_SHELF | false | 否 |

挂商家 orgId=5（广陵水乡人家）。封面 Unsplash HTTPS。原种子店 id=1 与既有 testData=true 演示店保留。

## 攻略

扬州侧已有 PUBLISHED 攻略挂广陵（regionId=5），articleCount=1，未再造。

## 意向

游客在新店 11/12 各提交演示意向（若接口字段兼容；以测服实际为准）。

## 硬约束

- 未改品牌色；无支付字段
- 南通不补到达标，专留未达标场景
- 密码不进本文、不进开发群
