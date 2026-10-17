# 开城测服执行清单 · MVP-1

| 项 | 内容 |
|----|------|
| 版本 | **v0.1**（执行清单首版） |
| 日期 | 2026-10-17 |
| 对齐用例 | [TEST-MVP-1-开城.md](./TEST-MVP-1-开城.md) **v0.2** |
| 契约 | TECH-DESIGN-MVP-1-开城（Origin PR **#12** / api#12）；路径以 TEST v0.2 为准 |
| 支付 | **本包无支付**：不做支付/定金/收银台/分账验收；仅 C08「没有」巡检 |
| 结论规则 | **仅已执行可下结论**；**2026-10-17 API 全量已跑**：见 [ACCEPTANCE-EXEC-MVP-1.md](./ACCEPTANCE-EXEC-MVP-1.md)；结果栏已回填 |
| 首开城市 | **扬州**；苏中一期已 OPEN 市保持 OPEN |
| 阈值 | **N=5**（上架可意向店）、**M=1**（已发布攻略）；未达标禁止开通（无 force-open） |
| 禁测 | 支付 / 定金 / 收银台 / 分账 / 库存锁房 / 区域运营角色拆分 / 观感债 R2 |

> 实现尚未合入前，「后端可测?=否」的写路径勿下 PASS。发现实现与 TECH/TEST 不一致时：改 TEST 并注明版本，在本文「差异留痕区」记一笔。

---

## 1. 测服前置检查

| # | 检查项 | 要求 | 勾选 |
|---|--------|------|------|
| P1 | 基线可达 | MVP-0 测服可用；门户 / 运营 / 商家三端可达 | ☑ API `/health` UP；前端未本跑 |
| P2 | 运营账号 | `POST /api/admin/auth/login` 得 Bearer（OPERATOR/AGENT）；种子账号可用 | ☑ |
| P3 | 游客 / 商家 Token | 游客 Token；商家 Token（只读意向抽检）就绪 | ☑ Demo 账号可用 |
| P4 | **种子三市已 OPEN** | TECH V4：`yangzhou` / `taizhou` / `nantong` 种子即为 **OPEN**（三项手勾 true），**即使店/攻略计数 &lt; N/M**。做「从零开通 / 未达标禁开」须先 **关城** 或换其他 CLOSED 市 | ☑ 已知悉；测中关南通/扬州 |
| P5 | **扬州补数** | 验收开通前（若已关再开）：上架可意向店（`ON_SHELF && !test_data`）≥ **5**；关联已发布攻略（`PUBLISHED`）≥ **1**；①④⑤ 手勾可勾；无对客可见爬取脏数据 | ☑ 终态 stores=5 arts=1 |
| P6 | 关城测未达标 | C01 / 从零开通前：目标市须为 **CLOSED** 且未达标（或人为造成未达标）；勿在种子 OPEN 态直接断言「未达标禁开」 | ☑ 以南通 CLOSED 测 C01 |
| P7 | 配置口径 | 开城单位=地级市；区县仅筛选项；N/M 以详情 `minStores`/`minArticles` 为准（默认 5/1） | ☑ minStores=5 minArticles=1 |

### Checklist 五项速查（开通门禁）

| # | 项 | 字段 | 类型 |
|---|----|------|------|
| 1 | 目的地数据齐 | `destinationReady` | 手勾 |
| 2 | 供给达标 | `supplyReady` | 自动（`storeCount >= minStores`）；不可手改 |
| 3 | 攻略达标 | `articlesReady` | 自动（`articleCount >= minArticles`）；不可手改 |
| 4 | 客服覆盖已说明 | `supportCovered` | 手勾 |
| 5 | 测数/脏数据已清理 | `dirtyDataCleared` | 手勾 |

开通条件：①④⑤ 均为真，且 ②③ 实时计数达标。

---

## 2. 建议执行顺序

| 序 | 阶段 | 用例 | 说明 |
|----|------|------|------|
| 1 | 冒烟 | **S1-HEALTH → S1-ADMIN → S1-YZ-READY → S1-OPEN → S1-NO-PAY** | 最小集；实现未合入时 S1-YZ-READY / S1-OPEN 可阻塞，勿虚报 PASS |
| 2 | 门禁与露出 | **C10 → C01 → C02** | 先看 checklist/计数，再验未达标禁开与达标开通；C01 前务必 close 或换 CLOSED 市 |
| 3 | 对客与关城 | **C03 → C04 → C05 → C06** | OPEN 列表意向 → 空态不串店 → 关城 410 → 多市筛与推荐 |
| 4 | 看板 | **C07** | 只读汇总 + CSV；无下钻、无佣金列 |
| 5 | 无支付 + 攻略 | **C08 → C09** | C08 为发版硬门禁；C09 攻略按城 |
| 6 | 余量 | 冒烟未覆盖的细项、矩阵勾选、权限 403 等 | 发版前补齐；观感债 R2 不进本清单 |

**建议首批开跑（可先于写接口合入）：** S1-HEALTH、S1-ADMIN、C08-4 / C08-5 / C08-6（沿用 MVP-0）、以及扬州补数 / 关城准备（P4–P6）。

---

## 3. 按用例 ID 勾选表

结果栏：空=待测；填 `PASS` / `FAIL` / `阻塞` + 简短备注（日期/执行人可选）。

### 3.1 冒烟 S1

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| S1-HEALTH | 测服基线 | `GET /health` | 服务 UP | PASS · 200 UP · 2026-10-17 |
| S1-ADMIN | 种子运营账号 | `POST /api/admin/auth/login` | 得运营 Bearer Token | PASS · 200 Token · 2026-10-17 |
| S1-YZ-READY | 运营 Token；扬州 cityId | `GET /api/admin/cities/{cityId}` | `storeCount`≥5、`articleCount`≥1；①④⑤ 可全勾 / `canOpen` 逻辑正确 | PASS · stores=5 arts=1 canOpen 逻辑正确 · 2026-10-17 |
| S1-OPEN | 达标；若已 OPEN 可先关再开 | `POST /api/admin/cities/{cityId}/open`；`GET /api/regions`；门户 Chip | 市 OPEN；regions / Chip 出现扬州 | PASS · close→open 200；regions 含扬州 · 2026-10-17 |
| S1-NO-PAY | 三端可达 | 跑 C08 矩阵 | 无支付 CTA；API 抽检 404/400 | PASS（API C08-4/5/6）；UI 矩阵 SKIP · 2026-10-17 |

### 3.2 C10 开通页 checklist 与 N/M 计数

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C10-1 | 运营 Token；扬州单市 | `GET /api/admin/cities/{cityId}` | 200；五项 checklist、`storeCount`/`articleCount`、`minStores`/`minArticles`、`canOpen`/`canClose`、`missingReasons`、`version` | PASS · 200 字段齐全 · 2026-10-17 |
| C10-2 | 补数过程；可 PATCH 手勾 | `PATCH /api/admin/cities/{cityId}/checklist`（仅手勾 + `version`）；体带 `supplyReady`/`articlesReady` | 成功 200；手改 ②③ → **400**「供给达标与攻略达标按实时计数自动勾选，不能手改」；N/M 实时更新 | PASS · 手勾 200；手改②③ → 400「…不能手改」 · 2026-10-17 |
| C10-3 | 运营开城列表 | `GET /api/admin/cities` | 200；行含市名、`openStatus`、`storeCount`、`articleCount`、`updatedAt` | PASS · 200 列表字段齐 · 2026-10-17 |
| C10-4 | 游客/商家 Token | `GET\|PATCH\|POST /api/admin/cities/**` | **403** | PASS · 游客/商家 403；无 Token 401 · 2026-10-17 |

### 3.3 C01 未达标禁止开通

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C01-1 | 运营 Token；目标市 **CLOSED** 且未达标（种子三市已 OPEN：先 close 或换 CLOSED 市） | `POST /api/admin/cities/{cityId}/open` body `{version}` | **409**；`message` 以「未达标，不能开通：」开头，缺项用「；」连接；市仍非 OPEN | PASS · 409「未达标，不能开通：…」南通 · 2026-10-17 |
| C01-2 | 同上；运营开城单市页 | UI：「开通此市」与缺项提示 | 按钮 disabled（`canOpen=false`）；列 `missingReasons`；**无**「强制开通」；②③ 不可点改 | SKIP · UI 未跑 |
| C01-3 | CLOSED；①④⑤ 全勾但供给=4（&lt;N）；攻略≥1 | `POST /api/admin/cities/{cityId}/open` + UI | **409**；提示供给未达标（含当前/需要） | SKIP · 未造 storeCount=4；供给缺项已由 C01-1 覆盖 |
| C01-4 | CLOSED；供给≥5、攻略≥1；①④ 勾但 `dirtyDataCleared=false` | `POST /api/admin/cities/{cityId}/open` + UI | **409**；缺「测数/脏数据已清理」类缺项 | PASS · 409 含「测数或脏数据未确认清理」 · 2026-10-17 |

### 3.4 C02 达标开通 → 门户露出

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C02-1 | 扬州（或目标市）CLOSED 且达标；运营 Token；`version` | `POST /api/admin/cities/{cityId}/open` body `{version}` | **200**/0；状态 OPEN；再开 → **409**「该市已开通」 | PASS · 200 OPEN；再开 409「该市已开通」 · 2026-10-17 |
| C02-2 | C02-1 成功后 | `GET /api/regions` | 200；仅 OPEN 市及下属启用区县（+有 OPEN 市的省）；含扬州；无 CLOSED 市 | PASS · regions 仅扬州+泰州 · 2026-10-17 |
| C02-3 | C02-1 后；门户首页 | UI：目的地 Chip | 出现「扬州」Chip；苏中已 OPEN 仍在；无 CLOSED Chip | SKIP · UI |
| C02-4 | 运营单市页已达标（CLOSED） | UI：「开通此市」→ 确认 | 成功反馈含「该市已对客可见」（或等价）；徽章变 OPEN | SKIP · UI |

### 3.5 C03 OPEN 市列表/详情/意向

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C03-1 | 扬州已 OPEN；有上架可意向店 | `GET /api/stores?regionId={扬州相关}` | 200；`destinationStatus=OPEN`；`emptyHint=null`；仅该市/区上架店；无串店/下架店 | PASS · OPEN / emptyHint=null · 2026-10-17 |
| C03-2 | 同上；游客 Token；扬州上架店 id | `GET /api/stores/{id}`；`POST /api/booking-intents` | 200；详情可读；意向 `PENDING`；**无**支付字段 | PASS · 详情 200；意向 PENDING 无支付字段 · 2026-10-17 |
| C03-3 | 门户 | UI：Chip → 列表 → 详情 → 提交意向 | 与一期一致；仅扬州上架店；成功页无支付/定金；对客无「上架」 | SKIP · UI |

### 3.6 C04 未开通市空态（不串店）

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C04-1 | 某市 CLOSED（可临时关扬州）；他市有上架店 | `GET /api/stores?regionId={CLOSED市}` | **200**；`items=[]`；`total=0`；`destinationStatus=CLOSED`；`emptyHint`=「该目的地暂未开通」；不串他市店 | PASS · CLOSED emptyHint「该目的地暂未开通」 · 2026-10-17 |
| C04-2 | 门户；CLOSED 市筛或深链 | UI 空态 | 文案含「该目的地暂未开通」；引导已开通/回首页；不串店；对客勿写「上架」；无支付词 | SKIP · UI |
| C04-3 | 首页 Chip / regions | `GET /api/regions` + UI Chip | 200；**不展示** CLOSED / 未开通市 | PASS · regions 无南通 · 2026-10-17 |

### 3.7 C05 关城 CLOSED + 直链不可订

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C05-1 | 某市已 OPEN；运营 Token；`version` | `POST /api/admin/cities/{cityId}/close` body `{version}` | **200**/0；状态 CLOSED；历史意向/店/攻略保留；再关 → **409**「该市未开通」 | PASS · close 200；再关 409「该市未开通」 · 2026-10-17 |
| C05-2 | C05-1 后 | `GET /api/regions` + 门户 Chip | 200；该市及区县不在 regions；Chip 消失 | PASS · regions 无南通 · 2026-10-17 |
| C05-3 | 市已 CLOSED；原上架店 id | `GET /api/stores/{id}` | **410**；message「已下架或不可用」；运营后台仍可编辑该店 | PASS · GET store 410「已下架或不可用」 · 2026-10-17 |
| C05-4 | 游客 Token；CLOSED 市店 id | `POST /api/booking-intents` | **410**「已下架或不可用」；不产生有效新单 | PASS · POST intent 410「已下架或不可用」 · 2026-10-17 |
| C05-5 | 门户；CLOSED 市店直链 | UI 详情页 | 与下架一致：不可订、无提交意向主 CTA；可留客服电话；无支付 CTA；对客勿写「上架」 | SKIP · UI |
| C05-6 | 运营单市页已 OPEN | UI：「关闭开通」→ 二次确认 | 确认文案含「门户将不再展示该目的地」（或等价）；确认后 CLOSED | SKIP · UI |

### 3.8 C06 多市 OPEN · 筛与推荐

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C06-1 | 苏中已 OPEN + 扬州已 OPEN | `GET /api/stores?regionId=...` + UI 筛 | 200；结果仅目标 OPEN 市上架店；`destinationStatus` 符合 TECH（OPEN/NONE） | PASS · OPEN/CLOSED destinationStatus · 2026-10-17 |
| C06-2 | 扬州店 `homeRecommended=true` 且市 OPEN；另有 CLOSED 市推荐店 | `GET /api/home/feed` | 200；首页推荐仅所属市 OPEN 的店；CLOSED 市推荐不对客；攻略位不含 CLOSED 市 | PASS · feed 无南通店 3/8 · 2026-10-17 |
| C06-3 | 门户首页 | UI：推荐楼层与 Chip | Chip 仅 OPEN 市；推荐无 CLOSED 市店；可点目的地仅 OPEN | SKIP · UI |

### 3.9 C07 种子指标看板

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C07-1 | 运营 Token；多市有意向样本 | `GET /api/admin/metrics/city-intents?window=7\|30\|ALL&cityId=` | 200；汇总行置顶 + 市行：意向/已确认/已落实；**无**下钻字段（无意向 id / 门店 id / href） | PASS · 7/30/ALL 合计置顶无下钻 · 2026-10-17 |
| C07-2 | 同上 | `GET /api/admin/metrics/city-intents/export` | 200 `text/csv`；列「市,意向数,已确认,已落实」；首数据行为合计；无佣金/支付金额列；已落实文案=「已落实」 | PASS · CSV「市,意向数,已确认,已落实」 · 2026-10-17 |
| C07-3 | 运营看板 UI | UI：时间窗 + 市/全部 | 三数展示；合计置顶；无钻进改单 | SKIP · UI |
| C07-4 | 未登录 / 非运营 | `GET /api/admin/metrics/city-intents` | **401** / **403** | PASS · 401 / 403 · 2026-10-17 |

### 3.10 C08 三端无支付巡检

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C08-1 | 门户 UI 可达 | UI：首页/列表/详情/意向成功/我的意向/攻略/未开通空态/CLOSED 直链 | 无支付/定金/收银台/钱包/积分 CTA；已落实只读且文案=「已落实」；对客勿写「上架」 | SKIP · UI |
| C08-2 | 运营 UI 可达 | UI：开城列表/单市/看板/意向跟进 | 无支付/定金/分账入口；开通/关闭无收款字段；CHECKED_IN 展示「已落实」 | SKIP · UI |
| C08-3 | 商家 UI 可达 | UI：本店意向 | 只读；无改状态/跟进写入；无支付/定金 | SKIP · UI |
| C08-4 | API | 抽检 `/api/payments`、`/api/checkout`、`/api/deposits` 等 | **404** 或不存在 | PASS · payments/checkout/deposits 等 404 · 2026-10-17 |
| C08-5 | 游客 Token；OPEN 市上架可订店 | `POST /api/booking-intents` body 塞 `amount`/`paymentStatus`/`deposit` | **400**（未知字段拒绝） | PASS · 未知支付字段 400 · 2026-10-17 |
| C08-6 | 商家 Token；本店有意向 | `GET /api/merchant/intents`；尝试任意写状态 | 读 200；写 **403** 或无写路由 | PASS · 读 200；写 404/405 · 2026-10-17 |

#### C08 冒烟矩阵（执行勾选）

| 端 | 无支付/定金/钱包 CTA | 已落实文案正确 | 商家意向只读 | UF-1 对客无「上架」 | 结果 |
|----|----------------------|----------------|--------------|---------------------|------|
| 门户 | ☐ C08-1 | ☐ C08-1 | — | ☐ C08-1 | SKIP UI |
| 运营 | ☐ C08-2 | ☐ C08-2 | — | —（后台可用「上架」） | SKIP UI |
| 商家 | ☐ C08-3 | — | ☑ C08-6 API | — | 部分（UI SKIP） |
| API 抽检 | ☑ C08-4 / C08-5 | — | ☑ C08-6 | — | PASS |

### 3.11 C09 攻略按城

| ID | 前置摘要 | 路径 | 期望码/文案要点 | 结果 |
|----|----------|------|-----------------|------|
| C09-1 | 扬州 OPEN；关联已发布攻略≥1 | `GET /api/articles` + UI | 200；可见该市攻略；列表不含 CLOSED 市攻略 | PASS · articles 200 含扬州相关 · 2026-10-17 |
| C09-2 | 某市 CLOSED；有关联已发布攻略 | `GET /api/articles`；`GET /api/home/feed`；`GET /api/articles/{id}` | 列表/feed 不主推 CLOSED 市攻略；直链详情仍 **200** | SKIP · 南通无关联已发布攻略 |
| C09-3 | 运营；扬州挂/确认攻略达 M | UI：单市页攻略计数 | `articleCount`/`minArticles` 达标绿；`articlesReady` 自动为真不可手改 | SKIP · UI |

---

## 4. 用例条数

| 分组 | 条数 |
|------|------|
| S1 冒烟 | 5 |
| C01 | 4 |
| C02 | 4 |
| C03 | 3 |
| C04 | 3 |
| C05 | 6 |
| C06 | 3 |
| C07 | 4 |
| C08 | 6（矩阵勾选不另计） |
| C09 | 3 |
| C10 | 4 |
| **合计** | **45** |

与 TEST-MVP-1 v0.2 统计一致。

---

## 5. 差异留痕区

实现与 TECH / TEST 不一致时：**改用例规则 → 更新 TEST 并 bump 版本**，再在此登记；勿在本清单单独改期望而不回写 TEST。

| 日期 | 用例 ID | 现象（实现 vs TECH/TEST） | 处理 | TEST 版本 |
|------|---------|---------------------------|------|-----------|
| 2026-10-17 | — | 测服 API 全量执行：无期望与实现对冲突需改 TEST；南通终态 CLOSED 记运维 | 结论见 ACCEPTANCE-EXEC-MVP-1；未改 TEST | v0.2 |

---

## 6. 不测提醒（勿误跑）

- 支付 / 定金 / 收银台 / 钱包 / 积分（仅 C08「没有」）
- 分账 / 佣金结算；库存日历 / 强锁房
- 区域运营账号拆分；强制开通 force-open
- 看板下钻到意向列表；一期观感债 R2（另抽）
- 发版 / 生产 / 删数：停等批准

---

## 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-10-17 | 自 TEST-MVP-1-开城 v0.2 整理可执行测服清单；路径与期望对齐 TECH#12；结果默认待测；无 commit |
| v0.1-exec | 2026-10-17 | 测服 API 全量回填结果栏；详见 ACCEPTANCE-EXEC-MVP-1；无 commit |
