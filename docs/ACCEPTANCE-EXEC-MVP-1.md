# 验收结论 · EXEC-MVP-1 开城（测服 API 全量）

| 项 | 内容 |
|----|------|
| 日期 | 2026-10-17（Asia/Shanghai） |
| 执行 | Grok Bot（API 为主；UI 用例标 SKIP） |
| 测服 | `http://106.12.204.206:8080` |
| jar / 静态 | api#15 / b25e3d6 + Flyway V4；静态 e4ae720（本跑以 API 为主） |
| 用例 | [TEST-MVP-1-开城.md](./TEST-MVP-1-开城.md) v0.2；清单 [EXEC-MVP-1-开城.md](./EXEC-MVP-1-开城.md) |
| 结论规则 | **仅已执行可下 PASS/FAIL**；未跑=待测/SKIP |

---

## 1. 摘要计数

| 结论 | 条数 |
|------|------|
| **PASS** | **30** |
| **FAIL** | **0** |
| **SKIP**（UI 未跑） | **13** |
| **SKIP**（数据前置不足，未单独造数） | **2**（C01-3、C09-2） |
| **阻塞** | **0** |
| **合计** | **45** |

- **无 FAIL。**
- 扬州：测中曾 close→达标 open 恢复；最终 **OPEN**，`storeCount=5`（≥N），`articleCount=1`。
- 南通：为 C01/C04/C05 关城后**无法再开**（供给 1/5、攻略 0/1）；最终 **CLOSED**。**@运维**：若需苏中三市均 OPEN，请补南通上架可意向店 ≥5 且已发布攻略 ≥1，再 `POST .../open`；或接受南通保持 CLOSED。
- 泰州：全程 **OPEN**（未关）。

---

## 2. 测服最终开城状态（2026-10-17 23:59 Asia/Shanghai）

| id | slug | openStatus | storeCount | articleCount | version | canOpen | 备注 |
|----|------|------------|------------|--------------|---------|---------|------|
| 2 | yangzhou | **OPEN** | 5 | 1 | 4 | false | 已恢复；checklist 五项均真 |
| 3 | taizhou | **OPEN** | 1 | 1 | 0 | false | 种子 OPEN，未动 |
| 4 | nantong | **CLOSED** | 1 | 0 | 5 | false | 验收关城后未达标，不能开回 |

`GET /api/regions` 仅含：江苏省 → 扬州市(+广陵区)、泰州市(+海陵区)；**无南通**。

---

## 3. 逐条结果（API）

### 3.1 冒烟 S1

| ID | 结果 | 证据摘要 |
|----|------|----------|
| S1-HEALTH | **PASS** | `GET /health` → HTTP **200**，`{"status":"UP","service":"nongjiale-api"}` |
| S1-ADMIN | **PASS** | `POST /api/admin/auth/login` → **200**/`code=0`，得 OPERATOR Bearer |
| S1-YZ-READY | **PASS** | `GET /api/admin/cities/2`：`storeCount=5`、`articleCount=1`、①④⑤ 真、②③ 自动真；CLOSED 时 `canOpen=true`，OPEN 时 `canOpen=false` |
| S1-OPEN | **PASS** | 扬州 close→open **200**/`openStatus=OPEN`；`GET /api/regions` 含扬州 |
| S1-NO-PAY | **PASS**（API） | C08-4/5/6 均 PASS；UI 矩阵见下 SKIP |

### 3.2 C10 checklist / 计数

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C10-1 | **PASS** | `GET /api/admin/cities/2` **200**：checklist 五项、`minStores=5`/`minArticles=1`、`canOpen`/`canClose`/`missingReasons`/`version` 齐全 |
| C10-2 | **PASS** | 手勾 PATCH **200**；体带 `supplyReady`/`articlesReady` → HTTP **400**，message **「供给达标与攻略达标按实时计数自动勾选，不能手改」** |
| C10-3 | **PASS** | `GET /api/admin/cities` **200**：行含市名、`openStatus`、`storeCount`、`articleCount`、`updatedAt` |
| C10-4 | **PASS** | 游客/商家读管理接口 **403**「无权限」；无 Token **401**「未登录」；游客 `POST .../open` **403** |

### 3.3 C01 未达标禁开

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C01-1 | **PASS** | 南通 CLOSED 后 `POST .../open` → **409**，message **「未达标，不能开通：供给未达标（当前 1 / 需要 5）；攻略未达标（当前 0 / 需要 1）」**；市仍 CLOSED |
| C01-2 | **SKIP** | UI 未跑 |
| C01-3 | **SKIP** | 未能将 `storeCount` 精确造为 4；供给未达标文案已由 C01-1 覆盖（当前/需要） |
| C01-4 | **PASS** | `dirtyDataCleared=false` 后 open → **409**，message 含 **「测数或脏数据未确认清理」**（与供给/攻略缺项用「；」连接） |

### 3.4 C02 达标开通

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C02-1 | **PASS** | 扬州 CLOSED 且达标后 open → **200**/`OPEN`；再开 → **409**「该市已开通」 |
| C02-2 | **PASS** | `GET /api/regions` **200**：仅 OPEN 市及区县；含扬州、泰州；**无**南通 |
| C02-3 | **SKIP** | UI Chip |
| C02-4 | **SKIP** | UI |

### 3.5 C03 OPEN 列表/意向

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C03-1 | **PASS** | `GET /api/stores?regionId=2` **200**：`destinationStatus=OPEN`，`emptyHint=null`，仅扬州下属店 |
| C03-2 | **PASS** | `GET /api/stores/1` **200**（无支付类字段）；`POST /api/booking-intents` **200**，`status=PENDING`，`intentNo` 以 NJL 开头 |
| C03-3 | **SKIP** | UI |

### 3.6 C04 未开通空态

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C04-1 | **PASS** | 南通 CLOSED：`GET /api/stores?regionId=4` **200**，`items=[]`，`total=0`，`destinationStatus=CLOSED`，`emptyHint=`**「该目的地暂未开通」** |
| C04-2 | **SKIP** | UI |
| C04-3 | **PASS** | regions **不展示** CLOSED 市（南通） |

### 3.7 C05 关城 + 410

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C05-1 | **PASS** | 南通 close **200**/`CLOSED`；再关 → **409**「该市未开通」 |
| C05-2 | **PASS** | regions 无南通及崇川区 |
| C05-3 | **PASS** | `GET /api/stores/3`（及 `/8`）→ **410**「已下架或不可用」；运营 `GET /api/admin/stores/3` 仍 **200** 可编辑 |
| C05-4 | **PASS** | `POST /api/booking-intents` storeId=3 → **410**「已下架或不可用」 |
| C05-5 | **SKIP** | UI |
| C05-6 | **SKIP** | UI |

### 3.8 C06 多市筛与推荐

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C06-1 | **PASS** | `regionId=2/3` → `destinationStatus=OPEN`；`regionId=4` → CLOSED 空态 |
| C06-2 | **PASS** | `GET /api/home/feed` **200**：推荐店 id 无南通店 3/8 |
| C06-3 | **SKIP** | UI |

### 3.9 C07 看板

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C07-1 | **PASS** | `GET .../city-intents?window=7|30|ALL` **200**：合计行置顶 + 市行；字段为意向/已确认/已落实计数；**无**意向 id / 门店 id / href |
| C07-2 | **PASS** | export **200** `text/csv`；首行表头 **「市,意向数,已确认,已落实」**；第一数据行「合计」；无佣金/支付列 |
| C07-3 | **SKIP** | UI |
| C07-4 | **PASS** | 无 Token **401**「未登录」；游客 **403**「无权限」 |

### 3.10 C08 无支付

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C08-1 | **SKIP** | 门户 UI |
| C08-2 | **SKIP** | 运营 UI |
| C08-3 | **SKIP** | 商家 UI |
| C08-4 | **PASS** | `/api/payments` `/api/checkout` `/api/deposits` `/api/wallet` `/api/pay` 均 **404**「接口不存在」 |
| C08-5 | **PASS** | body 塞 `amount`/`paymentStatus`/`deposit` → **400**「请求体格式不正确」 |
| C08-6 | **PASS** | `GET /api/merchant/intents` **200**；写路径 PATCH/PUT/POST status → **405**/**404**（无写路由） |

#### C08 矩阵（本跑）

| 端 | 无支付 CTA | 已落实文案 | 商家只读 | UF-1 | 结果 |
|----|------------|------------|----------|------|------|
| 门户 | SKIP UI | SKIP UI | — | SKIP UI | SKIP |
| 运营 | SKIP UI | SKIP UI | — | — | SKIP |
| 商家 | SKIP UI | — | PASS（API） | — | 部分 |
| API 抽检 | PASS C08-4/5 | — | PASS C08-6 | — | **PASS** |

### 3.11 C09 攻略

| ID | 结果 | 证据摘要 |
|----|------|----------|
| C09-1 | **PASS** | `GET /api/articles` **200**，含广陵相关已发布攻略；列表未出现「仅属南通」的独立攻略（南通 `articleCount=0`） |
| C09-2 | **SKIP** | 南通无关联已发布攻略，无法验「CLOSED 市攻略不主推 / 直链仍 200」完整场景；已发布详情抽样 `GET /api/articles/1|2|3` 均为 **200** |
| C09-3 | **SKIP** | UI |

### 附：乐观锁（清单硬规则）

| 场景 | 结果 |
|------|------|
| `POST .../close` 错 `version=99999` | **409**「数据已被修改」 |

---

## 4. 阻塞与风险

| 项 | 说明 |
|----|------|
| **南通最终 CLOSED** | 关城验收后 `canOpen=false`（店 1&lt;5、攻略 0&lt;1）。**@运维补数后 open**，或产品确认可保持 CLOSED。 |
| 扬州店数曾波动 | 开跑初 `storeCount=1`，中后达到 5（出现店 11/12/13/14 等）；达标开通在补齐后完成。 |
| Admin `storeCount` vs 门户 `total` | 门户 `regionId=2` total 可大于 admin `storeCount`（admin 计 `ON_SHELF && !test_data`）；门禁以 admin 计数为准，行为符合契约。 |
| 攻略列表筛城弱 | `?regionId=` / `?cityId=` 仍返回全部 3 篇；C09 按城过滤若产品期望更严，需另开缺陷/契约确认（本包未因此下 FAIL）。 |
| UI 全 SKIP | 发版前须补跑 C08 三端巡检与 Chip/开城页交互。 |
| 无 FAIL | API 路径与 409/400/410 文案与 TEST v0.2 / TECH 一致。 |

---

## 5. 账号与破坏性操作说明

- 运营：`13800000000` 登录成功；游客 / 商家 Demo 账号可读（见 private DEMO-ACCOUNTS-S5）。
- 破坏性仅限测服：`checklist` PATCH、南通 close、扬州 close→open；**未删库**。
- 扬州手勾曾短暂 `dirtyDataCleared=false`，已改回 `true`。

---

## 6. 回报一句话

**PASS 30 / FAIL 0 / SKIP 15（UI 13 + 数据 2）/ 阻塞 0**；**无需再为扬州补店**（已 ≥5 且 OPEN）；**需运维处理南通**：补店+攻略后 open，或确认保持 CLOSED。
