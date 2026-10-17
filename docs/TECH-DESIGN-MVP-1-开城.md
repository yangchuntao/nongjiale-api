# MVP-1 开城 · 技术设计

> 设计稿。待 UI + 产品双门禁 + 老板终验后再写业务代码。  
> 本文件只定契约。不改 Java、Flyway、控制器或测试。  
> 产品正文不在这里重复。范围、验收、UI 流程与门禁见：
>
> - `docs/PRODUCT-MVP-1-开城-范围.md`（Q6：②③按计数自动勾；Q7：看板不下钻）
> - `docs/PRODUCT-MVP-1-开城-UI流程要点.md`
> - `docs/PRODUCT-MVP-分期-已拍.md`
> - `docs/GATE-UI-MVP-1-开城-产品.md`
>
> 以上四份在 draft PR #11，合入 `main` 后路径不变。

已锁参数：首开城市扬州；N=5；M=1；未达标禁止开通；无支付、无定金；意向状态语义不改，`CHECKED_IN` 文案仍是「已入住」。

## ①概要设计

开通单位是市：`RegionLevel.CITY`（库内 `region.level = 2`）。区县只做筛选，不能单独开城。省不设开通态。

在现有 `region` 上增加市的开通态，不另建城市表。`enabled` 继续表示「地区节点是否可用、可维护」，不拿来表示开城。市另用 `open_status`：`CLOSED` / `OPEN`。省、区县该列为空，读路径忽略。

门户只把 **OPEN 的市** 及其下属区县放进目的地。关城后的店详情与「不可订」对齐现有下架：HTTP 410，信封与 `ShelfStatus != ON_SHELF` 相同。意向、门店、攻略不级联删除。

开通门槛：三个手勾项全为真，且实时店数 ≥ N、实时攻略数 ≥ M。②供给达标、③攻略达标不落库、不可手改，由计数推导。未达标 `POST .../open` 返回 409，中文缺项列表。没有强制开通。

N、M 走现有 `site_config` + `SiteConfigKey`，默认 5 和 1。本包不扩展现有站点配置的读写接口。

种子指标只读汇总：合计行 + 分市三列（意向 / 已确认 / 已入住），时间窗 7、30 或全部，可导出同结构 CSV。响应里不下钻，不给意向列表跳转契约。

商家端无新接口。支付、定金、库存日历、区域运营角色都不做。

苏中已有三市（扬州、泰州、南通）在迁移里标为 OPEN，门户行为与 MVP-0 一致。②③不伪造；店或攻略不够 N/M 时详情里显示未勾，但不会把这三市打回 CLOSED。

## ②详细设计

### 现状（按代码，实现时沿用）

| 事实 | 落点 |
|------|------|
| 技术栈 | Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、Redis 只做缓存、Bearer。枚举走码，不写魔法字符串 |
| `region` | `id, parent_id, slug, name, level, sort_order, enabled, seed_market`。无 `version`。种子：江苏省 + 扬州市 `yangzhou`、泰州市 `taizhou`、南通市 `nantong`，以及广陵 / 海陵 / 崇川 |
| 门户地区 | `GET /api/regions` → `RegionQueryService.enabledRegions()`，只要 `enabled=true`，缓存 `CacheName.REGION` 键 `enabled`，TTL `app.cache.region-ttl`（30 分钟） |
| 门店 / 攻略 | 已有 `region_id`。门店 `shelf_status`、`test_data`、`version`。攻略 `status`、`region_id`，无 `test_data` |
| 下架详情 | `StoreQueryService.detail` / `requireBookable`：非 `ON_SHELF` 抛 `ApiException(GONE, "已下架或不可用")`。`GlobalExceptionHandler` 包成 `{code:410, message, data:null}` |
| 列表 | `GET /api/stores` 的 `data` 是 `PageResult`：`items, total, page, pageSize`。上架查询不过滤 `test_data`；对客标记仍是「测试」 |
| 首页 | `CacheName.HOME_FEED` 键 `default`。推荐店要求 `home_recommended` 且上架。攻略取已发布前 6 篇，不按城滤 |
| 意向 | `IntentStatus`：`PENDING / CONFIRMED / RESCHEDULED / CHECKED_IN / CLOSED`。`CHECKED_IN` 标签「已入住」。有 `created_at`、`updated_at`，状态迁移记在 `intent_note`，没有单独的「状态变更时间」列 |
| 站点配置 | `SiteConfigKey`：`site.name`、`site.brand_color`、`site.contact_phone`。`GET/PUT /api/admin/site-config` 只覆盖这三项 |
| 运营鉴权 | `/api/admin/**`：`OPERATOR` 或 `AGENT` 的 Bearer。未登录 401，错角色 403 |
| 冲突文案 | 乐观锁失败已是 409「数据已被修改」 |
| 未知 JSON 字段 | `fail-on-unknown-properties: true` |
| 时钟 | `ClockConfig.ZONE = Asia/Shanghai` |
| CSV | 意向导出走 UTF-8 BOM、`text/csv`、`Content-Disposition: attachment` |

### 数据模型

下一版迁移计划为 **V4**（当前最新是 V3）。本设计不提交 SQL。

**`region.open_status`** `VARCHAR(32) NULL`。

枚举 `RegionOpenStatus`，`@EnumValue` 存码，JSON 也用码：

| 码 | 对运营/门户的含义 |
|----|-------------------|
| `CLOSED` | 未开通。徽章可用「未开通」 |
| `OPEN` | 已开通，对客可见 |

只允许市（`level=2`）非空。省、区县保持 NULL。读门户、算门槛时，NULL 与 `CLOSED` 同样视为未开通。不要把本枚举和意向的 `CLOSED`（「已关闭」）混用。

建议索引：`(level, open_status)`。

**`enabled` 与 `open_status` 分开：**

- `enabled=false`：节点不可维护。现有「地区不可用 / 地区不存在」校验不变。这种市不进开城列表，也不进门户。
- `open_status`：节点仍然可维护，只决定对客是否作为目的地。关城不改 `enabled`。

**`region_open_checklist`**，与市 1:1。只存三件手勾，不存②③。

| 列 | 类型 | 说明 |
|----|------|------|
| `city_id` | BIGINT PK，FK → `region.id` | 必须是市 |
| `destination_ready` | BOOLEAN NOT NULL DEFAULT FALSE | ①目的地数据齐，手勾 |
| `support_covered` | BOOLEAN NOT NULL DEFAULT FALSE | ④客服覆盖已说明，手勾 |
| `dirty_data_cleared` | BOOLEAN NOT NULL DEFAULT FALSE | ⑤测数/脏数据已清理，手勾 |
| `version` | BIGINT NOT NULL DEFAULT 0 | 乐观锁，见下 |
| `updated_at` | TIMESTAMP NOT NULL | 最近操作时间，开城列表用 |
| `updated_by` | BIGINT NULL，FK → `admin_user.id` | |

不建 `supply_ready`、`articles_ready`。这两项若落库，会和实时计数分叉，运营也能被误导成「手改已保存」。

没有 checklist 行时，三个手勾视为 false，`version` 视为 0。`GET` 详情不插入行。第一次成功的 PATCH / 开通 / 关城在版本匹配时插入。

**门槛配置**，沿用 `SiteConfigKey`：

| 枚举常量 | `config_key` | 种子值 |
|----------|--------------|--------|
| `OPEN_CITY_MIN_STORES` | `open_city.min_stores` | `5` |
| `OPEN_CITY_MIN_ARTICLES` | `open_city.min_articles` | `1` |

缺行、空串、非正整数：该键回退到 5 或 1，避免坏配置把开通打成 500。有效值以开城详情里的 `minStores` / `minArticles` 为准。`GET/PUT /api/admin/site-config` 的三个字段契约不改，本包不能在页面上改 N/M。

### 计数

市 C 的 **storeCount**（开通门槛，也是详情/列表上的供给数）：

- `store.shelf_status = ON_SHELF`
- `store.test_data = false`（`true` 不计入门户可见供给）
- `store.region.enabled = true`
- 店挂在 C 上，或挂在「父节点是 C」的区县上

直接挂省的店不计入任何市。区县上的上架店计入所属市。

**articleCount**：

- `article.status = PUBLISHED`
- `article.region_id` 非空，且该地区 `enabled = true`
- 攻略挂在 C 上，或挂在父节点是 C 的区县上

区县攻略计入该市的 M。`region_id` 为空，或只挂在省上的攻略，不计入任何市。攻略没有 `test_data`，不另做脏数据过滤；脏数据靠⑤手勾确认。

② `supplyReady = storeCount >= minStores`。③ `articlesReady = articleCount >= minArticles`。每次读详情、列表和开通前现算，不缓存进 checklist。

门户列表对 `test_data=true` 的店保持 MVP-0：市为 OPEN 且上架时仍返回，并带「测试」标记。门槛计数不含它们。⑤是运营确认，不是自动隐藏。

### 开通 / 关城

`canOpen` 为真，当且仅当：当前不是 OPEN，且①④⑤为真，且②③为真（即计数达标）。  
`canClose` 为真，当且仅当当前是 OPEN。

缺项 `missingReasons` 按这个固定顺序，只放未满足项：

1. `目的地数据未齐`
2. `供给未达标（当前 {storeCount} / 需要 {minStores}）`
3. `攻略未达标（当前 {articleCount} / 需要 {minArticles}）`
4. `客服覆盖未说明`
5. `测数或脏数据未确认清理`

开通成功的对客文案由前端写「该市已对客可见」。接口成功信封仍是 `code:0, message:ok`。关城成功同理，前端写「门户将不再展示该目的地」。

**POST open**

- 目标不是启用的市：不存在或 `level≠2` → 404「城市不存在」；`enabled=false` → 400「地区不可用」。
- 已经是 OPEN → 409「该市已开通」。不重复刷缓存。
- 门槛不满足 → 409，`message` 为 `未达标，不能开通：` 加上缺项，用中文分号 `；` 连接。`data` 为 null。例：`未达标，不能开通：供给未达标（当前 1 / 需要 5）；攻略未达标（当前 0 / 需要 1）`。
- 通过：同一事务里把 `open_status` 写成 OPEN，推进 checklist 的 `version` / `updated_at` / `updated_by`（手勾值不变）。提交后再失效缓存。

计数在持有 checklist 行锁之后、写状态之前重算。不提供强制开通参数；请求体里出现额外字段按未知字段拒绝。

**POST close**

- 同样只接受启用的市，错误码与 open 相同。
- 已经是 CLOSED（或 NULL）→ 409「该市未开通」。
- OPEN → 写成 CLOSED。不删意向、门店、攻略、checklist。不改上下架，不改 `enabled`，不清手勾。
- 关城后计数跌破与否都无关：关城本身无门槛。

已开通的市后来店或攻略跌破 N/M：**不自动关城**。详情里②③变为未勾，`canOpen=false`，`open_status` 仍为 OPEN，直到运营关城。再次开通必须重新过门槛。

### 乐观锁

`region` 不加 `version`。锁放在 checklist.`version`，MyBatis-Plus `@Version`，与门店、意向同一套插件。

PATCH、open、close 的请求体都带必填 `version`。

- 无 checklist 行且 `version=0`：插入后版本变为 1。
- 无行且 `version≠0`，或有行但版本不符：409「数据已被修改」。
- 成功写入后版本加 1。

open/close 即使不改三个布尔，也更新 `updated_at`，让列表「最近操作时间」前进。

### 运营接口

前缀 `/api/admin`。Bearer 规则与现网相同。成功 `{code:0, message:"ok", data}`。失败 `code` 等于 HTTP 状态，`data` 为 null，`message` 中文。枚举 JSON 用英文码。时间是上海时区 ISO-8601。

**GET `/api/admin/cities`**

不分页。只返回 `enabled=true` 且 `level=2` 的市，排序 `sort_order, id`。

`data` 为数组，元素：

| 字段 | 说明 |
|------|------|
| `id` | 市 id |
| `provinceName` | 父省名称，父节点不是省则为 null |
| `name` | 市名，如扬州市 |
| `slug` | 如 `yangzhou` |
| `openStatus` | `OPEN` 或 `CLOSED`（库内 NULL 对外也返回 `CLOSED`） |
| `storeCount` | 实时门槛计数 |
| `articleCount` | 实时门槛计数 |
| `updatedAt` | checklist.`updated_at`，无行则 null |

**GET `/api/admin/cities/{cityId}`**

`data`：

| 字段 | 说明 |
|------|------|
| 列表字段同上 | |
| `enabled` | 恒为 true（false 的市 400） |
| `minStores` / `minArticles` | 生效的 N、M |
| `checklist.destinationReady` | ①手勾 |
| `checklist.supplyReady` | ②推导，不落库 |
| `checklist.articlesReady` | ③推导，不落库 |
| `checklist.supportCovered` | ④手勾 |
| `checklist.dirtyDataCleared` | ⑤手勾 |
| `canOpen` / `canClose` | 见上 |
| `missingReasons` | 字符串数组，已开通且只是「已经开了」时为空数组；未达标时列出缺项 |
| `version` | 无行则 0 |
| `updatedBy` | 管理员 id 或 null |

**PATCH `/api/admin/cities/{cityId}/checklist`**

可写字段只有 `destinationReady`、`supportCovered`、`dirtyDataCleared`，外加必填 `version`。三者都是可空布尔：省略表示不改；至少要改一项，否则 400「请指定要更新的项」。

请求体若带 `supplyReady` 或 `articlesReady`（值不论真假）：**400**，`message` 为「供给达标与攻略达标按实时计数自动勾选，不能手改」。不落库，不改版本。选用 400 而不是忽略：和「未知字段即拒绝」一致，也避免运营以为手改已保存。实现时 DTO 要声明这两字段再主动拒绝；若 DTO 不声明，Jackson 会先返回笼统的「请求体格式不正确」，不符合本契约。

成功返回与 GET 详情相同的结构。不失效门户缓存：手勾在开通之前不影响对客。

**POST `/api/admin/cities/{cityId}/open`**  
**POST `/api/admin/cities/{cityId}/close`**

请求体只有 `version`。成功返回更新后的详情结构。

**GET `/api/admin/metrics/city-intents`**

查询：

| 参数 | 说明 |
|------|------|
| `window` | `7`、`30`、`ALL`。省略时默认 `7`（门禁：时间窗默认近 7 天）。其他值 400「参数不正确」 |
| `cityId` | 可选。省略表示全部启用的市。传入则必须是启用的市，否则 404「城市不存在」 |

这是汇总筛选，不是下钻。响应禁止出现：意向 id、门店 id、`href`、管理端意向列表的查询串、或任何「点市进入 `/api/admin/intents`」的字段。现有意向列表接口保持原样，本包不定义交接。

时间窗打在 **`booking_intent.created_at`**，不用 `updated_at`，也不扫 `intent_note`。没有可靠的「状态变更时间」列；种子指标看的是这段时间里产生的意向，以及这些单子**当前**状态。

- `7` / `30`：下界 = 应用时钟现在减去 7 或 30 天（滚动 24 小时，不是自然日零点），无上界。
- `ALL`：不加时间条件。

归属：意向 → 门店 → 地区。地区是市则归该市；是区县则归父市。挂省、地区缺失、或父节点不是市的意向不进任何市，也不进合计。

计数（同一批 `created_at` cohort 上的当前状态）：

| 字段 | 规则 |
|------|------|
| `intentCount` | 该窗内全部状态，含 `PENDING`、`RESCHEDULED`、意向 `CLOSED` |
| `confirmedCount` | 当前状态为 `CONFIRMED`。不含 `RESCHEDULED` |
| `checkedInCount` | 当前状态为 `CHECKED_IN`。标签含义仍是「已入住」 |

`data`：

| 字段 | 说明 |
|------|------|
| `window` | 实际使用的 `7` / `30` / `ALL` |
| `from` | 下界 ISO-8601；`ALL` 为 null |
| `rows` | 第一行是合计，其后是市 |

行字段：`cityId`（合计为 null）、`cityName`（合计为「合计」）、`intentCount`、`confirmedCount`、`checkedInCount`、`total`（合计 true）。

市行包含窗口内为零的启用市（OPEN 和 CLOSED 都在，历史单要能看见）。顺序 `sort_order, id`。`cityId` 有值时只有合计（等于该市）和这一市。合计等于列出的市行之和。没有「未归属」行。

**GET `/api/admin/metrics/city-intents/export`**

查询参数与汇总相同。`text/csv; charset=UTF-8`，UTF-8 BOM，附件名 `city-intents.csv`。列：`市,意向数,已确认,已入住`。第一数据行是合计。不是逐条意向。市的数量即行数，不设「导出条数过多」。

### 门户

**GET `/api/regions`**

响应元素仍是现有 `RegionItem`（`id, parentId, slug, name, level, sortOrder, seedMarket`），不增加 `openStatus`。过滤改为：

- `enabled=true`
- 市：`open_status=OPEN`
- 区县：父市启用且 OPEN
- 省：下面至少有一个启用且 OPEN 的市，否则整省不返回

CLOSED 市及其区县不出现。缓存键仍叫 `enabled`（`CacheAside` 只能按完整键删除）。加载逻辑改掉后，开通/关城必须删这个键；只改键名会把旧值留到 30 分钟 TTL。

**GET `/api/stores`**

在原分页字段上增加两个字段（旧客户端忽略即可）：

| 字段 | 值 |
|------|----|
| `destinationStatus` | `OPEN`、`CLOSED`、`NONE` |
| `emptyHint` | 仅 `CLOSED` 时为「该目的地暂未开通」，否则 null |

规则：

- 不传 `regionId`：`destinationStatus=NONE`。只返回「所属市 OPEN」的上架店。CLOSED 市的店不得混进默认列表。
- `regionId` 不是地区：400「地区不存在」。
- 地区 `enabled=false`：400「地区不可用」。
- 市为 CLOSED，或区县的父市不是 OPEN：200，`items=[]`，`total=0`，`destinationStatus=CLOSED`，`emptyHint` 如上。不回其他市的店。
- OPEN 的市：该市 + 其下 `enabled` 区县，且上架。`destinationStatus=OPEN`。
- OPEN 市下的区县：只筛该区县。`destinationStatus=OPEN`。
- 省：该省下 OPEN 市及其启用区县。`destinationStatus=NONE`。省下没有 OPEN 市时空列表，`emptyHint` 为 null（不是「暂未开通」）。

**GET `/api/stores/{id}` 与 POST `/api/booking-intents`**

在读 `CacheName.STORE_DETAIL` 之前判断，避免关城后仍命中可订缓存：

1. 店不存在 → 404「房源不存在」（现文案）。
2. 非 `ON_SHELF`，或所属市不是 OPEN（区县看父市；挂省或无法归到 OPEN 市，也算不可订）→ **410**，`message` 固定「已下架或不可用」，`data` 为 null。

与下架同一条 `ApiException`，不新造「未开通」正文，这样门户沿用下架态：无提交意向主按钮，客服电话继续用 `GET /api/config/public` 的站配电话。运营后台编辑不受这道 410 影响。

**首页 `GET /api/home/feed`**

- 推荐店：现有 `home_recommended` + 上架，再加所属市 OPEN。
- 攻略：已发布，且（`region_id` 为空，或只挂省，或所属市 OPEN）。CLOSED 市（含其区县）的攻略不占 6 条名额。SQL 过滤后再 `LIMIT 6`。

**GET `/api/articles`、GET `/api/articles/{id}`**

列表与首页同一规则：不主推 CLOSED 市攻略。已发布攻略的直链详情保持 200，不因所属市 CLOSED 改成 404。这是门禁写明的「正文可打开，不主推」。

商家入驻 `POST /api/merchant-applies` 仍只校验地区存在且 `enabled`。不要求 OPEN。不新增商家接口。商家意向只读、无支付字段，本包不动。

### 缓存

| 动作 | `REGION` / `enabled` | `HOME_FEED` / `default` | `STORE_DETAIL` |
|------|----------------------|-------------------------|----------------|
| PATCH checklist | 不删 | 不删 | 不删 |
| open / close | 删 | 删 | 不逐店删。详情在读缓存前做开通判断 |
| 现有代编门店、上下架、攻略发布 | 保持今天的 `AdminCache` | 同左 | 同左 |

Redis 删除失败只记日志，不回滚已经提交的开城事务（与 `AdminCache` 现注释一致）。缓存关闭或 Redis 不可用时仍直接读库。

### 审计

沿用 `AuditRecorder`。新增：

| `AuditAction` | 码 | 何时 |
|---------------|----|------|
| `CITY_CHECKLIST` | `CITY_CHECKLIST` | checklist 手勾保存成功 |
| `CITY_OPEN` | `CITY_OPEN` | 开通成功 |
| `CITY_CLOSE` | `CITY_CLOSE` | 关城成功 |

`AuditTarget` 增加 `REGION`。`target_id` 为市 id。`detail_json` 可含 `openStatus` 前后值与三个手勾，不写支付字段，不写 N/M 以外的计数快照也可省略。失败的 409 不写审计。

### 种子迁移（设计，V4）

1. `region` 增加 `open_status`。
2. 建 `region_open_checklist`。
3. `slug IN ('yangzhou','taizhou','nantong')` 且 `level=2`：`open_status='OPEN'`，checklist 三个手勾均为 true，`version=0`。
4. 其他 `level=2`：`open_status='CLOSED'`，checklist 三勾 false。当前种子没有第四座市，这一步是防以后的行。
5. 省、区县 `open_status` 保持 NULL。
6. 插入两个 `site_config` 键，值 `5` 和 `1`。不改站名、主色 `#4BA3D9`、客服电话。
7. 不插入假门店、假攻略，不把 `test_data` 改掉。

V3 每市只有 1 家手工上架店（表弟店在广陵，计入扬州），且没有攻略种子。迁移后扬州 `openStatus=OPEN`、`storeCount=1`、`articleCount=0`，②③为 false。门户仍出现扬州 / 泰州 / 南通。这是故意的：保住 MVP-0。验收「从未开通到开通」时，用例应先关城或使用一座 CLOSED 市，补到 N/M 且三项手勾后再开通。不要为了让②③变绿去改种子店数量。

### 实现时建议落点（仍不在本 PR 写代码）

- 枚举：`RegionOpenStatus`；`SiteConfigKey` 两个键；`AuditAction` / `AuditTarget` 如上。
- 实体与 Mapper：`Region.openStatus`；新实体 `RegionOpenChecklist`。
- 服务：运营开城服务（列表、详情、手勾、开、关、计数）；指标服务。门户改 `RegionQueryService`、`StoreQueryService`、`HomeFeedService`、`ArticleQueryService`。`requireBookable` 一处收口，详情和创建意向共用。
- 控制器：`/api/admin/cities/**`、`/api/admin/metrics/city-intents`。商家包不新增类。
- `AdminCache` 增加开/关时的地区与首页失效。
- 门户店列表用新的响应类型，保留原四个分页字段并加上 `destinationStatus`、`emptyHint`。不要改所有接口共用的 `PageResult` 语义。

### 不做

支付、定金、收银台、分账、钱包字段或 `/api/payments`。库存日历、锁房。区域运营角色。强制开通。改 `IntentStatus` 名称或「已入住」文案。商家新页面、新接口。问答、线路、专题、小程序。独立推荐位 CRUD。自动爬取。看板下钻到意向列表。

### 假设与未决

下列按现有代码和已拍产品写下。不再另造规则。

1. 时间窗打在 `created_at` 的滚动 7/30 天，默认 `7`。若以后要自然日，只改下界算法，仍用 `created_at`。
2. 「已确认」只数 `CONFIRMED`，不把 `RESCHEDULED` 算进去。
3. ②③不落库。PATCH 带这两字段返回 400，不忽略。
4. 未开通市攻略直链仍可打开；列表和首页不主推。
5. `test_data=true` 仍可出现在 OPEN 市的门户列表并标「测试」，只是不计入 N。若产品要硬隐藏，另拍。
6. 计数跌破 N/M 不自动关城。
7. 三座苏中市种子为 OPEN，即使当前计数不够 N/M。
8. 商家入驻提交仍接受 enabled 的 CLOSED 市。本包不加「全部地区」接口。若商家前端只调用过滤后的 `GET /api/regions`，未开通市会从下拉里消失；要不要另给入驻数据源，等产品拍，不在本包加接口。
9. 指标的 `cityId` 只筛汇总行。范围正文 C07 仍有「或合计+下钻」旧字样，以 Q7 和 `docs/GATE-UI-MVP-1-开城-产品.md` 为准：本期不下钻。本设计不去改产品文。
10. 挂到省上的店和攻略不计入市的 N/M，门户默认列表也不把「无法归到 OPEN 市」的店当作可订。

## ③改动点

**本 PR（设计稿）**

- 新增 `docs/TECH-DESIGN-MVP-1-开城.md`。
- `README.md` 文档列表下增加一行指针。
- 不改 `src/`、不改 `src/main/resources/db/migration`、不改测试、不改 `pom.xml`。

**终验通过后的实现（本 PR 不做）**

- V4：`region.open_status`、`region_open_checklist`、两行 `site_config`、苏中三市 OPEN + 三项手勾 true。
- 运营：城市列表、详情、三项手勾 PATCH、开通、关城、汇总指标与 CSV。
- 门户：地区树只留 OPEN 市；店列表/首页推荐按 OPEN 市；CLOSED 市店详情和创建意向走现有 410 下架信封；攻略列表与首页不主推 CLOSED 市，直链仍可读。
- 缓存：仅开通/关城失效地区缓存与首页缓存。
- 审计三个新动作。
- 实现阶段同步 `docs/ADMIN-API.md` 与 README 里门户/运营路径说明。本设计是契约源，避免两份文档在实现前分叉，所以本 PR 不改 `ADMIN-API.md`。

## ④影响点

- 门户行为：实现之后，CLOSED 市从目的地消失，默认店列表和首页推荐不再包含这些市。种子把扬州、泰州、南通标成 OPEN，因此 MVP-0 现有 Chip 和三家精选店仍在。新插入且未标 OPEN 的市默认对客不可见。
- 店详情：CLOSED 市与下架共用 410「已下架或不可用」。前端不要为开城另解析一套错误体。客服电话仍来自站配。
- 攻略：CLOSED 市不再出现在列表和首页；知道 id 仍能打开已发布正文。
- 运营：新页面依赖上述管理接口。开通按钮的禁用条件必须与 `canOpen` 一致（三项手勾 + ②③自动 + 计数）。②③控件不可编辑。看板行不可点进意向列表；改单仍走现有意向接口，本包不提供跳转参数。
- 商家：无新接口、无支付字段、意向只读。入驻校验仍看 `enabled`。
- 意向状态机与「已入住」文案不变。全站仍无支付路径；未知路径（含 `/api/payments`）维持 404「接口不存在」。
- 数据：关城不删历史意向和门店。乐观锁冲突沿用「数据已被修改」。
- 覆盖率：实现阶段新代码纳入现有 JaCoCo。`mvn test` 的 LINE / COVEREDRATIO 门禁仍是 **≥ 0.85**（`jacoco.line.minimum`，见 `docs/COVERAGE.md`）。只加不测的服务类会把整包拉过门禁。本 PR 无 Java 改动，不跑 `mvn test`。
- 缓存：地区 TTL 30 分钟。开通/关城若忘记删 `njl:region:enabled`，门户最长 30 分钟仍是旧目的地。checklist 手勾不应删这把键。
- 依赖：实现依赖 MVP-0 的地区、上架、意向、攻略、站点配置、商家入驻。不依赖 MVP-2 定金。
