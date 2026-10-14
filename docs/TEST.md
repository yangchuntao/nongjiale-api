# 农家乐正式版 v1.1 — 测试覆盖表（印程）

> 维护：资深测试工程师  
> 对齐：`PRODUCT-INCHENG-v1.1.md`（已冻结）· 验收 ID 逐条对照 `ACCEPTANCE-INCHENG-v1.1.md`  
> API：PR #4 草案 `docs/API.md`「v1.1 增量」（分支 `cursor/api-merchant-v1-1-9221`）。契约若改，以合并后的 API.md 为准再改本表。  
> 状态：**覆盖表草案**（v1.1 用例尚未执行，不得当作通过报告）  
> 更新：2026-10-14（上海）

编号与验收表一致。A3、A9、B6 的期望已按产品拍板改写；验收表 B6 修订在 PR #1（产品在改 `ACCEPTANCE-INCHENG-v1.1.md`），合并前以本表拍板为准。本表不新增、不改号。A/B/C/E 状态仍为 **待测**。Demo/P1 的历史 PASS 只在文末附录，**不算** v1.1 的 A/B/C 已过，也不重验商家端与三端范围。

## 1. 范围

### 本期验证

- 门户：搜索 → 地区 → 列表（筛选/排序）→ 详情 → 须登录提交意向 → 成功页（配置客服电话）→ 我的意向只读（不可自助取消）→ 攻略/资讯。见 A0–A9（含 A1b）。
- 运营：入驻审核（仅运营）、上下架申请审核（方案 B）、意向跟进、全站内容、站点配置、商家账号启用/停用。见 B1–B4、B7–B8。
- 客服：意向跟进 + 房源只读；不可入驻审、不可管商家账号、不可改站点配置、不可审上下架、不可改首页推荐标记。见 B4–B6、B8。
- 商家第三端：入驻申请后由运营开通；多店只维护本主体；上架须申请；意向 M1 只读且手机脱敏。见 A9、C1–C7。
- 角色隔离由 A1b、A5、B5、B6、B8、C1、C4、C5、C6 覆盖，不另编号。

### 明确不做（标不测）

支付/定金收银台、锁房/实时库存、积分、结算/对账/提现/分账、未登录下单与短信查单、问答、私人定制、专题频道、地图、IM、App、门票、动态码登录、商家端营销券/合同电子签。这些功能本身 **不测**；是否漂移用 D1–D6 抽检。契约不提供 `/api/payments`、`/api/merchant/settlements`、`/api/inventory`、`/api/sms`、`/api/im`，这些路径保持 404。独立推荐位 CRUD（`home_slot` 一类单独资源）一期 **不做、不测**；首页推荐只验房源上的标记和排序，见 B6。

## 2. 覆盖表

方式只取 `API`、`UI`、`人工` 之一，本轮不改。状态除 D 节外均为 **待测**。

统一响应 `{code,message,data}`。下表未写死状态码的成功调用：HTTP 200 且 `code=0`。失败时 HTTP 与 `code` 对齐：未登录 **401**（`message`「未登录」），角色不够 **403**（`message`「无权限」，商家改意向另有专用文案），摸到别人的店/意向 **404**（不回对方字段）。鉴权头 `Authorization: Bearer <token>`。`storeId` = 门户/运营的 `homestayId`。

门户列表沿用 Demo，没有单独验收 ID：`GET /api/regions`（`data` 为数组）、`GET /api/home/feed`、`GET /api/homestays`（仅已上架；`regionId` / `regionSlug` / `keyword` / `priceMin` / `priceMax` / `page` / `pageSize`；门户 URL 的 `q` 映射为 `keyword`）。v1.1 另加可选 `tag`、`sort=default|price|sales`。

`mustChangePassword=true` 时，除 `POST /api/merchant/auth/login`、`GET /api/merchant/auth/me`、`POST /api/merchant/auth/password` 外，其余商家接口 **403**「请先修改初始密码」。C 段执行前先改密，不单列编号。

### A · 门户（A0–A9）

| ID | 前置 | 操作 | 期望 | 方式 | API | 状态 |
|----|------|------|------|------|-----|------|
| A0 | 未登录 | 详情点「立即预订」 | 进入登录；登录成功后回跳该房源预订表单 | UI | 回跳无单独接口。上架详情 `GET /api/homestays/{id}` → 200。登录 `POST /api/auth/login` body `phone`,`password` → 200，`data.token` | 待测 |
| A1 | 已登录 + 上架可订门店 | 提交合法意向（日期合法、人数≥1） | 成功页：单号 + 可拨打**配置客服电话**；库中待跟进且含 userId；文案不暗示已锁房/已付款 | UI | 门户 Bearer `POST /api/booking-intents`，body `homestayId`,`checkIn`,`checkOut`,`guests`≥1,`contactName`,`contactPhone` → 200，`data.intentId`、`data.intentNo`、`data.status=PENDING`、`data.contactPhone` 等于 `GET /api/config/public` 的 `contactPhone`（站点客服电话，不是游客填写的手机）。库中该单 `userId` 为当前用户。锁房/付款文案仍看成功页 | 待测 |
| A1b | 无登录态 | 调提交意向接口 | 401，不入库 | API | 无 token `POST /api/booking-intents` → **401**，`code=401`，`data=null`，不入库 | 待测 |
| A2 | 已登录 + 同用户同门店同到店日已有单 | 再提交/弱网重试 | 不新增；返回已有单 | API | 同一门户用户再 `POST /api/booking-intents`。幂等键 `userId:homestayId:checkIn` → 200，`data.intentId` 与已有单相同，条数不增 | 待测 |
| A3 | 门店下架或未审上架 | 打开详情或提交 | 详情 **410**；不可订再提交意向 HTTP **409**，不入库 | UI | `GET /api/homestays/{id}` → **410**，`code=410`，「该农家乐暂不可预订」，且不出现在 `GET /api/homestays`。再 `POST /api/booking-intents` → **409**，`code=409`，不入库 | 待测 |
| A4 | 已登录 | 非法日期（离店≤到店）或人数 0 | 字段错误，不入库 | API | 门户 Bearer `POST /api/booking-intents`：`checkOut`≤`checkIn`，或 `guests=0` → **400**，`code=400`，不入库 | 待测 |
| A5 | 已登录 | 打开「我的意向」 | 只见本人单据；可见状态；**无**取消/改状态入口 | UI | `GET /api/me/intents`、`GET /api/me/intents/{id}` 只含本人，字段含 `status`。他人 id → **404**。不提供 `POST /api/me/intents/{id}/cancel`（打到即 **404**），无改状态接口 | 待测 |
| A6 | 列表无结果 | 打开列表 | 空态，非报错页 | UI | `GET /api/homestays?keyword=...`（由门户 `q` 映射）→ 200，`code=0`，`data.items=[]`，`data.total=0`，不是 4xx/5xx | 待测 |
| A7 | 任意 | 走攻略/资讯列表→详情 | 可打开运营已发布内容 | UI | 访客 `GET /api/articles`、`GET /api/articles/{id}` 对已发布 → 200。草稿或不存在 → **404**。发布前置（仅 OPERATOR）：`POST /api/admin/articles`，再 `POST /api/admin/articles/{id}/status` body `status=PUBLISHED` | 待测 |
| A8 | 未登录 | 点「在线咨询」 | 先登录再回跳；已登录合法留言进待跟进 | UI | 回跳无单独接口。未登录 `POST /api/contact-messages` → **401**。已登录合法 body（`content`,`contactName`,`contactPhone`）→ 200，`data.status=PENDING`（Demo 基线已有该响应；v1.1 未改此路径） | 待测 |
| A9 | 访客 | 提交商家入驻申请（必填齐） | 申请进入待审；申请人**未**自动获得商家权限；未开通就商家登录 HTTP **401** | UI | `POST /api/merchant-applications`，body 含 `subjectName`,`contactName`,`phone`,`proposedStoreName`,`regionId` → 200，`data.applicationId`，`data.status=PENDING`。B1 通过前用该手机 `POST /api/merchant/auth/login` → **401**，`code=401`，与无有效商家账号同口径，不签发 token。门户 token 调 `GET /api/merchant/stores` → **403**。已有 `PENDING` 再提 → **409** | 待测 |

### B · 运营 / 客服（B1–B8）

| ID | 前置 | 操作 | 期望 | 方式 | API | 状态 |
|----|------|------|------|------|-----|------|
| B1 | 运营 | 通过入驻申请 | 商家账号可用且绑定该主体；驳回须原因且可再提 | UI | OPERATOR `POST /api/admin/merchant-applications/{id}/approve` → 200，`data.status=APPROVED`，`data.merchantId`，`data.loginPhone`，`data.initialPassword`（只此一次），`data.mustChangePassword=true`。随后 `POST /api/merchant/auth/login` 可登录且 `merchantId` 为该主体。`POST .../reject` 的 `reason` 为空 → **400**；有原因 → `REJECTED` 且 `rejectReason` 可见，申请人可再 `POST /api/merchant-applications` 得新的 `PENDING`。非 `PENDING` 再审 → **409** | 待测 |
| B2 | 运营 | 审核商家上架申请通过 | 门店对游客可订；驳回后仍不可订且原因可见 | UI | OPERATOR `POST /api/admin/listing-applications/{id}/approve`（`action=PUBLISH`）→ 门店 `status=ON_SALE`，`GET /api/homestays/{id}` → 200 且进入列表。`POST .../reject` 的 `reason` 为空 → **400**；有原因则门店 `status` 不变，商家 `GET /api/merchant/listing-applications/{id}` 可见 `rejectReason`，游客详情仍 **410**。非 `PENDING` → **409** | 待测 |
| B3 | 运营 | 审核下架申请通过 | 门店对游客不可订 | UI | OPERATOR `POST /api/admin/listing-applications/{id}/approve`（`action=UNPUBLISH`）→ 门店 `status=OFF_SALE`，`GET /api/homestays/{id}` → **410**，且退出 `GET /api/homestays` | 待测 |
| B4 | 运营/客服 | 意向确认 + 备注 | 状态已确认；备注时间线可见 | API | OPERATOR 或 AGENT `POST /api/admin/intents/{id}/status`（别名 `POST /api/admin/booking-intents/{id}/status`）body `status=CONFIRMED`,`note` → 200。`GET /api/admin/intents/{id}` 的 `status=CONFIRMED` 且 `notes` 含该备注。`version` 不一致 → **409**，不覆盖 | 待测 |
| B5 | 客服 | 尝试入驻审 / 管商家账号 / 改站点配置 | **无权限**，数据不变 | API | AGENT 调用以下接口 → **403**，`code=403`，目标数据不变：`GET/POST /api/admin/merchant-applications` 及 `.../approve`、`.../reject`；`GET /api/admin/merchants`、`POST /api/admin/merchants/{merchantId}/enabled`、`POST .../reset-password`；`GET/PUT /api/admin/site-config` | 待测 |
| B6 | 已上架门店；运营与客服各有账号 | 运营改房源「首页推荐」标记与排序；客服尝试改同一标记 | 门户首页推荐楼层按该标记与排序展示；客服改标记 **403** 且数据不变；OPERATOR 可改。独立推荐位接口不做、不测 | API | 不调用独立推荐位 / `home_slot` CRUD（不做、不测）。推荐是房源上的「首页推荐」标记和排序字段，不是单独资源。OPERATOR 改标记与排序成功后，`GET /api/home/feed` 的首页推荐楼层含该已上架房源，顺序与排序字段一致。AGENT 改同一标记 → **403**，`code=403`，标记与排序不变。客服改归属仍 `POST /api/admin/homestays/{id}/owner` → **403**，归属不变 | 待测 |
| B7 | 运营 | 改站点客服电话并保存 | 成功页/`tel:` 展示新号码（配置生效） | UI | OPERATOR `PUT /api/admin/site-config` 写入 `contactPhone` → 200。`GET /api/config/public` 的 `contactPhone` 为新号码。之后 `POST /api/booking-intents` 的 `data.contactPhone` 同新号码。成功页 `tel:` 仍看 UI。正式站名/主色/电话见 E1，本条用占位号即可 | 待测 |
| B8 | 运营 | 上下架申请队列 | 仅运营可审；客服不可审（默认） | API | OPERATOR `GET /api/admin/listing-applications` → 200。AGENT 的 `GET` 以及 `POST .../{id}/approve`、`POST .../{id}/reject` → **403**，申请与门店 `status` 不变 | 待测 |

### C · 商家隔离（C1–C7）

| ID | 前置 | 操作 | 期望 | 方式 | API | 状态 |
|----|------|------|------|------|-----|------|
| C1 | 商家 A 已入驻 | 登录商家端 | 只见本主体门店；不见商家 B 门店 | UI | `POST /api/merchant/auth/login` body `phone`,`password` → 200，`data.role=MERCHANT`，`data.merchantId` 为 A。`GET /api/merchant/stores` 只含该 `merchantId`。`GET /api/merchant/stores/{B的storeId}` → **404**，响应体无 B 的字段。query/body 传入 B 的 `merchantId` → **403** | 待测 |
| C2 | 商家 A | 改自家门店图文/套餐并保存 | 后台资料更新；**未**经上架审则游客仍不可订（若原为下架） | UI | `PUT /api/merchant/stores/{storeId}`（`name`,`gallery`,`notice` 等 + `version`）→ 200；`version` 冲突 → **409**。套餐 `POST/PUT /api/merchant/stores/{storeId}/packages`；标签 `PUT .../tags` body `tags`。body 里的 `merchantId`/`status`/`recommendWeight` 忽略，上下架不走 PUT。原为下架或草稿时 `status` 仍非 `ON_SALE`，`GET /api/homestays/{id}` → **410** | 待测 |
| C3 | 商家 A + 门店下架 | 提交上架申请 | 进入运营待审；游客端仍不可订直至 B2 | UI | `POST /api/merchant/stores/{storeId}/listing-applications` body `action=PUBLISH` → 200，申请 `status=PENDING`。此时 `GET /api/homestays/{id}` 仍 **410**，直到 B2 通过。不提供 `POST /api/merchant/stores/{storeId}/status`（**404**）。商家 token 调 `POST /api/admin/homestays/{id}/status` → **403**。同一 `action` 已有 `PENDING` 再提 → **409** | 待测 |
| C4 | 商家 A | 打开意向列表 | **只读**本店意向；手机脱敏；**无**改状态/写跟进入口 | UI | `GET /api/merchant/intents`、`GET /api/merchant/intents/{id}` 只含本主体门店。字段为 `contactPhoneMasked`（前 3 后 4，如 `138****5678`），响应不得出现完整 `contactPhone`。他人意向 id → **404**。写接口见 C5 | 待测 |
| C5 | 商家 A | 调改意向状态或写跟进接口 | 拒绝（403/业务错），状态不变 | API | `POST /api/merchant/intents/{id}/status` 与 `POST /api/merchant/intents/{id}/notes` → **403**，`message`「商家不可修改意向或写跟进」，状态与备注不变。单据不存在、属于本主体或属于其他商家都是 **403**（不用 404 探单）。同路径 PATCH/PUT 同样 **403** | 待测 |
| C6 | 商家 A | 尝试编辑商家 B 门店或审入驻 | 拒绝，数据不变 | API | `PUT /api/merchant/stores/{B的storeId}`（及其 packages/tags）→ **404**，B 的资料不变。商家 token `POST /api/admin/merchant-applications/{id}/approve` 或 `.../reject` → **403**，申请状态不变 | 待测 |
| C7 | 商家 A | 商家端是否出现支付/结算/提现入口 | **无**（范围抽检） | 人工 | 无成功路径。`/api/payments`、`/api/merchant/settlements` 以及提现/分账保持 **404**，不做半成品。页面入口仍人工看 | 待测 |

### D · 不做项抽检（D1–D6）

功能不在本期。验收表本段只有「检查 / 期望」，无单独前置；下表前置填「—」，操作与期望照抄。状态标 **不测（抽检）**，不记功能通过。不补成可执行的 API 用例。

| ID | 前置 | 操作 | 期望 | 方式 | 状态 |
|----|------|------|------|------|------|
| D1 | — | 支付/定金收银台 | 三端均无 | 人工 | 不测（抽检） |
| D2 | — | 积分抵现 | 无 | 人工 | 不测（抽检） |
| D3 | — | 结算/对账/提现 | 无 | 人工 | 不测（抽检） |
| D4 | — | 问答/私人定制/专题频道 | 无独立频道（资讯可有） | 人工 | 不测（抽检） |
| D5 | — | 地图找店 / IM / App / 门票 | 无 | 人工 | 不测（抽检） |
| D6 | — | 未登录下单 + 短信查单 | 无 | 人工 | 不测（抽检） |

### E · 配置到位后重测（E1）

站名 / 主色 / 客服电话正式值仍待老板提供。未到位前 **不执行** E1；到位后重测。占位配置下的电话改写见 B7，B7 通过不等于 E1 通过。

| ID | 前置 | 操作 | 期望 | 方式 | API | 状态 |
|----|------|------|------|------|-----|------|
| E1 | 老板已提供站名/主色/电话 | 写入站点配置 | 门户展示站名与主色；成功页与页头电话为正式值 | UI | 到位后才测。OPERATOR `PUT /api/admin/site-config` body `siteName`,`primaryColor`（`#RRGGBB`）,`contactPhone` → 200。`GET /api/config/public` 三个字段等于正式值。页头站名、主色、成功页与页头 `tel:` 仍看 UI | 待测 |

## 3. 执行结论

**v1.1 尚未执行。** A0–A9、A1b、B1–B8、C1–C7、E1 全部 **待测**；D1–D6 为 **不测（抽检）**。没有任何 v1.1 通过结论。附录里的 Demo/P1 PASS 只证明当时的门户意向主路径，**不能**写成商家端、入驻审核、上下架方案 B、三端隔离或本表已过。

## 4. 依赖与已定码

- 上表 API 列对齐 PR #4 草案，并写入下面三条拍板。不是执行结果。发版前停等批准。
- E1 等站名、主色、客服电话正式值到位后再测。未到位前不执行。B7 只覆盖占位客服电话。
- **已按产品拍板定码**（原先三条契约缺口关闭）。验收表 B6 修订见产品正在改的 `ACCEPTANCE-INCHENG-v1.1.md` / PR #1；该 PR 合并前，A3、A9、B6 以本表为准。
  1. **B6**：一期不单开推荐位 CRUD。独立推荐位接口不做、不测。改为验房源「首页推荐」标记 + 排序字段，并生效到门户首页推荐楼层（`GET /api/home/feed`）。客服改推荐标记 **403**，OPERATOR 可改。
  2. **A3**：不可订门店再 `POST /api/booking-intents` → HTTP **409**，不入库。详情 `GET /api/homestays/{id}` 仍 **410**。
  3. **A9**：入驻未开通就 `POST /api/merchant/auth/login` → HTTP **401**，与无有效商家账号同口径。
- 文案与回跳没有单独字段，API 不能单独判过：A0 / A8 的登录回跳，A1 成功页「不暗示已锁房/已付款」，A6 的空态页面，E1 的站名与主色展示。

## 5. 变更记录

| 日期 | 变更 |
|------|------|
| 2026-10-14 | 按 `ACCEPTANCE-INCHENG-v1.1.md` 重写覆盖表（A0–A9、B1–B8、C1–C7、D1–D6、E1）；状态均为待测或抽检。Demo/P1 执行结果降为附录，不计入 v1.1。 |
| 2026-10-14 | 按 PR #4 `docs/API.md` v1.1 增量补 A/B/C/E 的方法、路径与关键状态码。状态仍为待测；D 仍为不测（抽检）。 |
| 2026-10-14 | 按产品拍板改 A3（提交 409 / 详情 410）、A9（未开通商家登录 401）、B6（首页推荐标记与排序，不测独立推荐位接口）。三条原契约缺口改为已定码。状态仍为待测。 |

## 附录 · Demo/P1 历史结论

> 仅存档。执行日 2026-10-14 13:22（上海）。范围是 Demo：方案 A、须登录、无我的订单、成功页电话 `13651814168`、**无商家端**。  
> **不得**把下表 PASS 填回第 2 节，也不得视为 A/B/C 已重跑。

| 历史用例 | 结果 | 与 v1.1 的关系（不是通过证据） |
|----------|------|--------------------------------|
| T0 UI | PASS | 仅门户登录回跳，接近 A0 的旧界面，未覆盖 v1.1 商家与配置电话 |
| T1 UI + `T1BookingIntentApiTest` 数据层 | PASS | 成功页 `intentId=3` / `intentNo=NI20261014545744` + `tel:13651814168`。电话是 Demo 写死值，不是 E1 |
| T1b API | PASS | 无 Token 提交 401。旧接口，不是 v1.1 商家增量契约 |
| T2 API | PASS | 同键不新增。旧意向接口 |
| T3b API | PASS | 下架详情 410 / 提交拒绝。无「未审上架」（A3 后半） |
| T4 API | PASS | 非法日期或人数 0。旧接口 |
| T5 / T5b API | PASS | 客服确认、备注、旧 version 409。Demo 管理端，未区分 v1.1 运营/客服权限（B5、B8） |
| T6 API | PASS | 客服改房源/权限 403。不是 B5 入驻审 / 商家账号 / 站点配置 |
| T7 UI | PASS | `/search?q=zzzzempty999` 空态。参数名是 `q` |
| T8b API | PASS | 已登录留言入库 PENDING。T8 登录回跳 UI 当时未验 |

API 套件（当时）：`T1BookingIntentApiTest` 7/7，`T5AdminIntentFollowUpTest` 3/3，`T8ContactMessageApiTest` 3/3。可选未验：T3a 下架详情文案、T8 咨询登录回跳 UI、T9 断网/500。演示账号（仅内用）：游客 `13800000001/password`；运营 `operator/password`；客服 `agent/password`。
