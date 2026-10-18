# MVP-1.5 广告位 · 技术设计

> 设计稿。待产品门禁 + 老板 TECH 终验后再写业务代码。  
> 本文件只定契约。不改 Java、Flyway、控制器或测试。  
> 范围、验收、页面与门禁见：
>
> - `docs/PRODUCT-MVP-1.5-广告位-范围.md`（C-Ad1～C-Ad5；老板 A★）
> - `docs/PRODUCT-MVP-分期-已拍.md`（本包 = MVP-1.5，插在开城后、小程序前）
> - `docs/DESIGN-MVP-1.5-广告位.md`（已入库；轮播、排序、Chip、外链）
> - `docs/UI-PAGES-MVP-1.5-广告位.md`（已入库；A-BANNER、A-BANNER-EDIT、P-HOME delta）
> - `docs/GATE-UI-MVP-1.5-广告位-产品.md`（UI 门禁 PASS，只作参考，本文件不另写 GATE）
>
> 开城前提：`docs/TECH-DESIGN-MVP-1-开城.md`、`docs/PRODUCT-MVP-1-开城-范围.md`。市开通态已在 `main`。

已锁参数：仅运营后台可配；仅门户首页 Banner；可配图、跳转链接、启停、排序、投放范围 `ALL` / `CITY`；`CLOSED` 市不能当有效定向，定向后若市关掉则不对客展示；启停刷新即生效；无启用且合法的条目时 Banner 区折叠隐藏。排序**数值越小越靠前**。全站条与目的地 Chip 无关。0 条隐藏、1 条静帧、≥2 条轮播由前端做，本接口只给有序集合。无支付、无买位、无曝光点击统计。不改意向状态机，`CHECKED_IN` 文案仍是「已入住」。

图片本包选定 **URL 填入**，不做上传接口。取舍见「图片」。

## ①概要设计

新增一张首页 Banner 表 `home_banner`。一条记录是运营投放的一张图加一个跳转。不挂在 `site_config` 上：站配仍是站名、主色、客服电话三项，本包不扩展 `GET/PUT /api/admin/site-config`。

投放范围两个码：

- `ALL`：全站。只要启用，就进门户首页集合，与游客当前是否选了某市无关。
- `CITY`：指定一座市。写入时该市必须已开通（`CityRules.isOpen`）。门户只在该市**当前仍开通**时把这条放进集合。

门户读接口 `GET /api/home/banners` **不接收城市参数**。游客 Chip、URL 上的市，都不参与过滤。集合规则与 DESIGN §3.2、UI-PAGES P-HOME 相同：启用，且（全站，或指定市且该市现为 OPEN），再按排序升序。因此首页可以同时出现全站条和每一座当前 OPEN 市的条。南通若被关成 CLOSED，指向南通的条不出现，即使它仍是启用。

运营侧是标准 CRUD，外加行内启停。排序没有单独的拖拽接口，写在创建/更新的 `sortOrder` 上。列表默认也按这个升序，方便和首页对照（C-Ad3）。

商家端无新路由。支付、定金、钱包、买位、竞价、曝光、点击都不建表、不加字段、不加路径。

## ②详细设计

### 现状（按代码，实现时沿用）

| 事实 | 落点 |
|------|------|
| 技术栈 | Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、Redis 只做缓存、Bearer。枚举走码，不写魔法字符串 |
| 迁移 | 最新是 `V4__city_open.sql`。本包下一版是 **V5**。本设计不提交 SQL |
| 市 | `region.level = 2`，`open_status` 为 `OPEN` / `CLOSED`（NULL 读成 CLOSED）。`enabled` 仍是节点可维护，不是开城。种子扬州、泰州、南通都是 OPEN |
| 开通判断 | `CityRules.isOpen`：市、`enabled=true`、`open_status=OPEN` 三者同时成立 |
| 门户地区 | `GET /api/regions` 只留 OPEN 市及其启用区县。缓存 `CacheName.REGION` 键 `enabled`，TTL 30 分钟 |
| 首页 | `GET /api/home/feed` 缓存 `CacheName.HOME_FEED` 键 `default`，TTL `app.cache.home-feed-ttl`（5 分钟）。响应只有推荐店和攻略，没有 Banner |
| 封面 | 门店、攻略的 `coverUrl` 是最长 512 的字符串。全库没有 `MultipartFile`、没有对象存储、没有上传控制器。V3 种子店的 `cover_url` 为 NULL |
| 运营鉴权 | `/api/admin/**`：`OPERATOR` 或 `AGENT` 的 Bearer。未登录 401，游客或商家令牌 403 |
| 运营角色 | `AdminRole.OPERATOR` 标签「运营」，`AGENT` 标签「客服」。现网管理接口不在方法上再拆这两个角色 |
| 分页 | `PageResult`：`items, total, page, pageSize`。`PageQuery` 默认 page=1、pageSize=20，最大 100，非法 400「分页参数不正确」 |
| 未知 JSON 字段 | `fail-on-unknown-properties: true`，落到 400「请求体格式不正确」 |
| 类型不匹配 | 400「参数不正确」 |
| 冲突文案 | 乐观锁失败已是 409「数据已被修改」。本表不加 `version`，不用这句 |
| 时钟 | `ClockConfig.ZONE = Asia/Shanghai`。时间 JSON 为 ISO-8601 |
| 成功信封 | `{code:0, message:"ok", data}`。失败 `code` 等于 HTTP 状态，`data` 为 null，`message` 中文 |
| 未知路径 | 含 `/api/payments`，404「接口不存在」，不是 500 |
| 意向 | `IntentStatus.CHECKED_IN` 标签「已入住」。本包不改这个枚举，也不改状态迁移 |

### 数据模型

**`home_banner`**

| 列 | 类型 | 说明 |
|----|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | |
| `title` | VARCHAR(64) NULL | 运营标题/备注。空则门户 `alt` 由前端用「活动宣传」 |
| `image_url` | VARCHAR(512) NOT NULL | 图片地址，仅 http(s) |
| `link_url` | VARCHAR(512) NOT NULL | 站内路径或 http(s) 外链 |
| `scope` | VARCHAR(32) NOT NULL | `BannerScope`：`ALL` / `CITY` |
| `city_id` | BIGINT NULL，FK → `region.id` | `ALL` 必须为 NULL；`CITY` 必须是市 |
| `sort_order` | INT NOT NULL | 越小越靠前。同值再按 `id` 升序 |
| `enabled` | BOOLEAN NOT NULL | 启用。JSON 用布尔，不用第二套字符串枚举 |
| `created_by` | BIGINT NOT NULL，FK → `admin_user.id` | |
| `updated_by` | BIGINT NULL，FK → `admin_user.id` | |
| `created_at` | TIMESTAMP NOT NULL | |
| `updated_at` | TIMESTAMP NOT NULL | |

建议索引：`(enabled, sort_order, id)`，供门户有序扫描；`(scope, city_id)` 供按市核对。

`ALL` 与 `city_id` 的互斥放在服务里校验，不靠数据库 CHECK（本地 H2 `MODE=MySQL` 与 MySQL 8 都要能跑 V5）。

不建 `impression_count`、`click_count`，不建统计表，不建价格、订单、广告主、商家外键。

**枚举 `BannerScope`**，`@EnumValue` 存码，JSON 也用码：

| 码 | 含义 |
|----|------|
| `ALL` | 全站。忽略目的地 Chip |
| `CITY` | 指定 `city_id` 那一座市 |

**启停**用布尔 `enabled`，与 DESIGN / UI-PAGES 的 `enabled=true` 和 Switch 一致。不增加 `ENABLED` / `DISABLED` 码。Java 里用 `Boolean.TRUE` / `Boolean.FALSE` 判断，不比较字符串 `"true"`。

文案集中在枚举 `BannerMessage`（状态 + 中文句子）。服务只引用枚举常量，不在业务方法里散落中文字面量。`PageQuery` 已有的「分页参数不正确」继续用现成句子，不复制进这个枚举。

### 图片（Q2 取舍）

现网没有可复用的运营上传。门店和攻略封面都是调用方自己填的 URL。

本包选定：**只收 `imageUrl`，不新增上传接口。**

- 与 `coverUrl` 同一上限 512，不引入桶、鉴权、体积和 Content-Type 嗅探。
- 运营在 A-BANNER-EDIT 填写 `http://` 或 `https://` 地址；失焦预览由前端做。
- 前端本包不要调用不存在的上传路径。UI 句「请上传图片或填写图片地址」在接口侧收成「请填写图片地址」。
- 相对路径、`data:`、`javascript:` 不能当图片：门户换域名就会裂图。
- 不探测图片是否返回 200。地址格式合法就可以存，裂图由前端占位（UI：缩略图失败占位）。

以后若要真正上传，另开包，不改这张表的 `image_url` 含义。

### 链接

必填。本包只校验形态，不请求目标，不对 4xx/5xx 弹窗（产品规则 5、UI-PAGES）。

| 形态 | 规则 | 点击（前端，接口不执行跳转） |
|------|------|------------------------------|
| 站内 | 以单个 `/` 开头，第二字不是 `/`，无空白、无反斜杠，可带 `?` 与 `#` | 同标签页 |
| 外链 | 以 `http://` 或 `https://` 开头，中间无空白 | 新标签页，`rel=noopener noreferrer` |

`//host`、`javascript:`、`data:` 拒绝。`http` 不拒绝（产品 Q4）。接口不返回 `openInNewTab`：前端按上表看 `linkUrl` 前缀，避免两套规则。

### 排序与启停

`sortOrder` 为整数，闭区间 **0～1000000**。新建省略时默认 **100**（UI 建议值）。更新必填。

列表与门户的次序都是 `sort_order ASC, id ASC`。同序用 `id`，先插入的在前，不依赖时钟。不提供 `sort` 查询参数，避免运营列表和首页各排各的。

行内启停：

- `POST .../enable`、`POST .../disable`，无请求体。
- 已经是目标状态：200，返回当前详情，**不再写一条审计**。
- 启停不改排序、范围、图和链接。
- 启用一条 `CITY` 且该市现已不是 OPEN：409，文案见下。运营要改范围或改市，走 PUT，不能靠启停把非法定向重新打开。

关城不回写 `enabled`。条可以仍是启用，只是门户滤掉。运营列表用 `cityOpen=false` 配 UI 文案「市已关闭·不对客」。再次把该市开通后，这条若仍启用，会重新进首页集合，无需再点启用。

### 写入校验

创建与更新共用同一套。顺序固定，命中即返回，不收集多条。

1. `imageUrl` 空白 → 400「请填写图片地址」。超长 → 400「图片地址过长」。不是 http(s) → 400「图片地址须为 http 或 https」。
2. `linkUrl` 空白 → 400「请填写跳转链接」。超长 → 400「跳转链接过长」。形态不符 → 400「跳转链接须为站内路径或 http(s) 外链」。
3. `title` 空白当成 NULL。超 64 → 400「标题过长」。
4. `sortOrder` 缺、非整数、或超出 0～1000000 → 400「请输入整数排序值」。非整数 JSON 会先被现有处理器收成「请求体格式不正确」，实现时用整数类型即可。
5. `scope` 缺 → 400「请选择投放范围」。码不是 `ALL`/`CITY` → 现有「请求体格式不正确」（Jackson 枚举失败）。
6. `scope=ALL` 且 `cityId` 非空 → 400「全站投放不能指定市」。
7. `scope=CITY` 且 `cityId` 为空 → 400「请选择已开通的市」。
8. `cityId` 不是启用中的市：不存在、或 `level≠2` → 404「城市不存在」；`enabled=false` → 400「地区不可用」；不是 OPEN（含 NULL）→ 400「投放市未开通，请改选全站或其他已开通市」。
9. 创建时 `enabled` 省略 → `true`。更新时 `enabled` 必填，缺则 400「请指定是否启用」。

`CITY` 指向未开通市时，即便 `enabled=false` 也拒绝。CLOSED 不能变成新的有效定向（DESIGN 硬锁）。已经落库、后来被关城的行：PUT 仍走第 8 步，保存前必须改成全站或另一座 OPEN 市；只想停用则走 `POST .../disable`。

未知字段 400「请求体格式不正确」。

### 运营接口

前缀 `/api/admin/banners`。Bearer 与现网管理接口相同：`OPERATOR` 与 `AGENT` 都可以。本包不把客服从运营后台拆出去。游客令牌、商家令牌 403；未登录 401。产品「仅运营」用于排除游客和商家，并保证商家端没有配置入口。

成功体里的市名字段读当前 `region`，不在 Banner 行上冗余存名称。

**详情字段（创建、更新、启停、单条 GET 共用）：**

| 字段 | 说明 |
|------|------|
| `id` | |
| `title` | 无则 null |
| `imageUrl` | |
| `linkUrl` | |
| `scope` | `ALL` 或 `CITY` |
| `cityId` | `ALL` 为 null |
| `citySlug` | 市的 `region.slug`，如 `yangzhou`。UI 稿里的 cityCode 用这个字段，不另造 `cityCode` |
| `cityName` | 如「扬州市」。`ALL` 为 null |
| `cityOpen` | `CITY` 且 `CityRules.isOpen` 为 true，否则 `CITY` 为 false。`ALL` 为 null |
| `sortOrder` | |
| `enabled` | 布尔 |
| `createdAt` / `updatedAt` | 上海时区 ISO-8601 |

**GET `/api/admin/banners`**

分页，`PageQuery`。固定 `sort_order ASC, id ASC`。筛选都可选：

| 参数 | 说明 |
|------|------|
| `enabled` | `true` / `false`。省略表示启用和停用都要 |
| `scope` | `ALL` / `CITY`。其他值 400「参数不正确」 |
| `keyword` | 对 `title`、`link_url` 做包含匹配，先 trim。空串当省略。超过 64 字 400「参数不正确」 |
| `page` / `pageSize` | 与现网相同 |

`data` 是 `PageResult`。元素字段与详情相同。空列表 `items=[]`，不是 404。UI 空态文案「暂无 Banner，点击新建投放」由前端写。

**GET `/api/admin/banners/{id}`**

没有这条 → 404「Banner 不存在」。`id` 不是数字 → 400「参数不正确」。

**POST `/api/admin/banners`**

请求体：`title` 可空，`imageUrl`、`linkUrl`、`scope` 必填，`cityId` 按范围，`sortOrder` 可空（默认 100），`enabled` 可空（默认 true）。成功 200，`data` 为详情。`createdBy` 记当前管理员，响应不另加这个字段。

**PUT `/api/admin/banners/{id}`**

全量更新。`imageUrl`、`linkUrl`、`scope`、`sortOrder`、`enabled` 必填；`title`、`cityId` 可空。不存在 → 404「Banner 不存在」。成功返回详情。

**POST `/api/admin/banners/{id}/enable`**  
**POST `/api/admin/banners/{id}/disable`**

无请求体。不存在 → 404。`enable` 时若 `scope=CITY` 且市现不是 OPEN → **409**「投放市未开通，请改选全站或其他已开通市」。`disable` 不受市开通态限制。已是目标状态 → 200，不重复审计。

**DELETE `/api/admin/banners/{id}`**

不存在 → 404「Banner 不存在」。成功 `data` 为 `{ "id": number }`，与攻略删除相同。物理删除，不留软删标记。确认文案「删除后不可恢复，确认删除该 Banner？」在前端。

### 门户

**GET `/api/home/banners`**

公开，无需登录。不要挂到 `/api/me/**`。不要加进 `GET /api/home/feed`：feed 有 5 分钟缓存，Banner 失败还必须能单独把该区藏起来（UI：接口失败静默隐藏，不挡搜索和意向）。

无查询参数。实现不要声明 `cityId` / `regionId` / `slug`。多出来的查询串按 Spring 默认忽略，**不得据此过滤**。

`data` 为数组，不是分页。元素：

| 字段 | 说明 |
|------|------|
| `id` | |
| `title` | 无则 null。前端 `alt`：有标题用标题，否则「活动宣传」 |
| `imageUrl` | |
| `linkUrl` | |
| `scope` | |
| `cityId` / `citySlug` / `cityName` | `ALL` 三个都是 null |
| `sortOrder` | |
| `enabled` | 恒为 true |

入集条件（服务端做完，不把 CLOSED 条交给前端再滤）：

1. `enabled=true`
2. `scope=ALL`，或者 `scope=CITY` 且 `CityRules.isOpen(该市)`
3. 排序 `sort_order ASC, id ASC`

例子：扬州 OPEN、泰州 OPEN、南通被运营关成 CLOSED。启用条为「全站 A」「扬州 B」「南通 C」「泰州 D」。无论 Chip 停在扬州、泰州还是未选，返回顺序都是按排序的 A、B、D。C 不在响应里。Chip 从扬州改到泰州，集合不变。

0 条：200，`data: []`。不返回「暂无广告」。前端不渲染 Banner 区。1 条静帧、≥2 条约 5 秒轮播、外链新开页，都是前端，接口不带播放参数。

条数预期很小，**不走 Redis**。每次读库，启停和关城后刷新即可看到。不新增 `CacheName`。不要把 Banner 塞进 `HOME_FEED` 的 `default` 键，否则最长 5 分钟仍是旧图，C-Ad2 会失败。

关城、开通沿用现有 `AdminCache.evictCityOpen()`，本包不要求它再删 Banner 键（因为没有这把键）。

### 缓存

| 动作 | 地区缓存 | 首页 feed | Banner |
|------|----------|-----------|--------|
| Banner 增删改、启停 | 不删 | 不删 | 无缓存 |
| 开城 / 关城 | 保持今天 | 保持今天 | 读库，关城后 CITY 条立刻不进集合 |
| 改站配、改店、改攻略 | 保持今天 | 保持今天 | 无关 |

### 审计

沿用 `AuditRecorder`。`AuditTarget` 增加 `HOME_BANNER`。`target_id` 为 Banner id。

| `AuditAction` | 码 | 何时 |
|---------------|----|------|
| `BANNER_CREATE` | `BANNER_CREATE` | 创建成功 |
| `BANNER_UPDATE` | `BANNER_UPDATE` | 更新成功 |
| `BANNER_ENABLE` | `BANNER_ENABLE` | 从停用变成启用 |
| `BANNER_DISABLE` | `BANNER_DISABLE` | 从启用变成停用 |
| `BANNER_DELETE` | `BANNER_DELETE` | 删除成功 |

`detail_json` 可含 `scope`、`cityId`、`enabled`、`sortOrder`。不写支付、不写点击数。失败的 400/404/409 不写审计。幂等启停不写。

### 错误码与文案

HTTP `code` 仍是数字状态，不另造 `BANNER_*` 业务码。句子只从 `BannerMessage` 出。

| HTTP | 场景 | message |
|------|------|---------|
| 400 | 没填图片 | 请填写图片地址 |
| 400 | 图片超长 | 图片地址过长 |
| 400 | 图片不是 http(s) | 图片地址须为 http 或 https |
| 400 | 没填链接 | 请填写跳转链接 |
| 400 | 链接超长 | 跳转链接过长 |
| 400 | 链接形态不对 | 跳转链接须为站内路径或 http(s) 外链 |
| 400 | 没选范围 | 请选择投放范围 |
| 400 | 全站却带了市 | 全站投放不能指定市 |
| 400 | 指定市却没选 | 请选择已开通的市 |
| 400 | 市未开通（含写入 CLOSED） | 投放市未开通，请改选全站或其他已开通市 |
| 400 | 市节点停用 | 地区不可用 |
| 400 | 排序非法 | 请输入整数排序值 |
| 400 | 标题超长 | 标题过长 |
| 400 | 更新没带 enabled | 请指定是否启用 |
| 400 | 查询枚举/关键词非法、路径 id 不是数字 | 参数不正确 |
| 400 | 分页非法 | 分页参数不正确 |
| 400 | 未知字段、坏 JSON、非法 scope 码 | 请求体格式不正确 |
| 401 | 未登录或令牌失效 | 沿用现网 |
| 403 | 游客或商家令牌打管理接口 | 沿用现网 |
| 404 | 无此 Banner | Banner 不存在 |
| 404 | cityId 不是市 | 城市不存在 |
| 409 | 启用时，库内 CITY 的市已不是 OPEN | 投放市未开通，请改选全站或其他已开通市 |
| 410 | 本包不使用 | 下架店详情仍是现有 410「已下架或不可用」 |

### 种子（V5 建议，可随表一起写）

在建表之后插入两行，`created_by` 用种子运营 `admin_user.phone = '13800000000'`（与 V3 攻略相同）。`city_id` 用 `slug = 'yangzhou'` 的市，不写死自增 id。

| title | scope | 市 | sort_order | enabled | image_url | link_url |
|-------|-------|----|------------|---------|-----------|----------|
| 苏中逛一逛 | ALL | NULL | 10 | true | `https://example.com/njl/banner-all.png` | `/` |
| 扬州周末 | CITY | 扬州 | 20 | true | `https://example.com/njl/banner-yangzhou.png` | `/?city=yangzhou` |

`example.com` 只占位，验收时图打不开是预期。C-Ad1 的「看得见的图」由用例或运营改成可访问的 https 地址。两条的排序 10、20 用来看「越小越前」，新建默认 100 不会插到它们前面。

不要插入南通定向。V4 把南通标成 OPEN，和扬州、泰州一样，为的是保住 MVP-0 门户。C-Ad4 要「南通 CLOSED」时，用例先调用现有关城，或自建一座 CLOSED 市。V5 不得把南通改回 CLOSED。

不改站名、主色 `#4BA3D9`、客服电话。不改意向、门店、攻略。

### 实现时建议落点（仍不在本 PR 写代码）

- 枚举：`BannerScope`、`BannerMessage`；`AuditAction` 五个码；`AuditTarget.HOME_BANNER`。
- 实体与 Mapper：`HomeBanner`。
- 服务：运营 Banner 服务（列表、详情、创建、更新、启停、删除、校验）；门户查询只读入集。
- 控制器：`/api/admin/banners/**`、`GET /api/home/banners`。商家包不新增类。`SecurityConfig` 保持 `/api/admin/**` 现有角色，门户 GET 落在现有 `permitAll`。
- 不改 `HomeFeedService` 的响应结构，不改 `AdminCache.evictCityOpen` 的键，不改 `IntentStatus`。
- 实现阶段再补 `docs/ADMIN-API.md` 与 README 的路径说明。本文件是契约源，实现前不改那两份，避免分叉。

### 不做

支付、定金、收银台、买位、竞价、合同、钱包字段、`/api/payments`。曝光、点击、报表、计费表。列表插卡、详情广告、视频、DSP、第三方 SDK。商家端页面与接口。上传接口与对象存储。拖拽排序接口。按游客当前市过滤 Banner。改 `CHECKED_IN` 的码或「已入住」文案。改开城门槛、checklist、N/M。自动关城时去改写 Banner 的 `enabled`。

### 假设（已拍板的工程选择）

下列在产品允许的缺省里定死，供门禁核对。接口与字段对齐已入库的 PRODUCT、DESIGN、UI-PAGES。

1. 图片只收 URL，本包无上传。
2. `citySlug` 即 `region.slug`。没有 `cityCode` 字段。
3. 门户集合与 Chip 无关；CITY 看的是目标市是否 OPEN，不是游客选中的市。
4. 门户 Banner 不缓存。
5. 表无乐观锁。并发保存以后写为准。409 只用于「启用时市已不再开通」。
6. `OPERATOR` 与 `AGENT` 都能调用管理接口。
7. 种子两行使用 example.com 占位图；南通种子保持 OPEN。
8. 轮播 5 秒、静帧、隐藏、新标签页是前端行为，接口不返回这些开关。

## ③改动点

**本 PR（设计稿）**

- 新增 `docs/TECH-DESIGN-MVP-1.5-广告位.md`。
- 不改 `README.md`、`docs/ADMIN-API.md`、`src/`、`src/main/resources/db/migration`、测试、`pom.xml`。
- 不新增 GATE 文档。UI 门禁已在 `docs/GATE-UI-MVP-1.5-广告位-产品.md`。

**终验通过后的实现（本 PR 不做）**

- V5：建 `home_banner`；建议插入全站一条、扬州 OPEN 一条。
- 运营：列表、详情、创建、更新、启用、停用、删除。
- 门户：`GET /api/home/banners`，服务端按启用 +（ALL 或 OPEN 市）过滤，升序。
- 审计五个新动作。
- 实现阶段同步 `docs/ADMIN-API.md` 与 README。补测试覆盖 C-Ad1～C-Ad5，并保住 JaCoCo 行覆盖率。

## ④影响点

- 门户首页：实现之后多一次公开 GET。0 条得到空数组，前端折叠 Banner 区，搜索、目的地 Chip、推荐、意向主路径保持原样。feed 的字段和缓存键不变。
- 按市：全站条在未选市和已选市时都在。指定市的条在该市仍为 OPEN 时就出现在首页，不要求游客先点中这座市。市一旦 CLOSED，对应条从门户消失，运营列表仍能看到，并带 `cityOpen=false`。
- 开城数据：关城、开通不删 Banner 行，也不改 `enabled`。再次开通后，仍启用的 CITY 条会回到首页。V5 不改扬州 / 泰州 / 南通的 OPEN 种子。C-Ad4 的 CLOSED 市由用例自己关，或另用一座未开通的市。
- 运营：新页面走上述管理接口。行内启停不带版本号。编辑保存是全量 PUT。排序旁注「数字越小越靠前」与 `sortOrder` 一致，默认 100。
- 图片：前端只做 URL 输入和预览。没有上传 API。
- 商家：无新接口、无广告配置、无买位。商家令牌访问 `/api/admin/banners/**` 为 403。
- 意向：状态机与「已入住」不变。全站仍无支付路径；未知路径（含 `/api/payments`）维持 404「接口不存在」。
- 站配与主色：不改。Brand 仍是 `#4BA3D9`。本包不增加绿色品牌色，也不在接口里下发新配色。
- 覆盖率：实现阶段新代码纳入现有 JaCoCo。`mvn test` 的 LINE / COVEREDRATIO 门禁仍是 **≥ 0.85**（`jacoco.line.minimum`，见 `docs/COVERAGE.md`）。只加不测的服务类会把整包拉过门禁。本 PR 无 Java 改动，不跑 `mvn test`。
- 依赖：依赖 MVP-0/1 的运营登录、`region` 开通态、`CityRules.isOpen`、审计和管理员种子。不依赖 MVP-2 小程序，也不依赖 MVP-3 定金。
