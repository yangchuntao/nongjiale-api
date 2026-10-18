# 运营 API 字段表（FE-4）

基路径 `/api/admin`。除登录外需要 `Authorization: Bearer <token>`，角色 `OPERATOR` 或 `AGENT`。

成功：

```json
{ "code": 0, "message": "ok", "data": {} }
```

失败时 `code` 等于 HTTP 状态，`data` 为 null。`message` 是中文。

| HTTP | 何时 |
|------|------|
| 401 | 未登录，或令牌已被吊销 |
| 403 | 游客或商家令牌 |
| 400 | 校验失败，文案取第一条字段错误 |
| 404 | 申请、商家、门店、意向、攻略不存在 |
| 409 | 重复审核、非法状态迁移、乐观锁冲突、手机号已是商家 |
| 410 | 本任务不改。门户对下架门店的详情和意向仍是 410 |

列表 `data`：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 当前页 |
| total | number | 总条数 |
| page | number | 从 1 起，默认 1 |
| pageSize | number | 默认 20，最大 100 |

时间是 `Asia/Shanghai` 的 ISO-8601 字符串，日期是 `YYYY-MM-DD`。枚举 JSON 使用英文码。

## 登录（已有）

`POST /api/admin/auth/login`

| 字段 | 类型 | 必填 |
|------|------|------|
| phone | string | 是 |
| password | string | 是 |

`data`：`token`、`expiresAt`、`adminId`、`role`、`displayName`。

`GET /api/admin/auth/me` 的 `data`：`adminId`、`phone`、`role`、`displayName`。

全新库的演示种子（正式环境必须更换；旧明文 `Demo123456` 已停用）：

| 端 | 手机号 | 口令 |
|----|--------|------|
| 运营 | `17606838920` | `123456` |
| 商家主号 | `17606838921` | `123456` |
| 游客 | `17606838922` | `123456` |

商家主号挂在「广陵表弟农家」。不再提供演示副号。已有库不会被迁移改写这些账号。

## 入驻审核

`GET /api/admin/merchant-applies`

查询：`status`（`PENDING` / `APPROVED` / `REJECTED`）、`page`、`pageSize`。

`items[]`：

| 字段 | 类型 |
|------|------|
| id | number |
| orgName | string |
| contactName | string \| null |
| contactPhone | string \| null |
| plannedStoreName | string \| null |
| regionId | number \| null |
| description | string \| null |
| licenseUrls | string \| null |
| status | `PENDING` \| `APPROVED` \| `REJECTED` |
| rejectReason | string \| null |
| reviewedBy | number \| null |
| reviewedAt | string \| null |
| merchantOrgId | number \| null |
| createdAt | string |

`POST /api/admin/merchant-applies/{id}/approve`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| password | string | 否 | 8–64 位。不传则生成临时密码 |

`data`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 申请 id |
| status | string | `APPROVED` |
| merchantOrgId | number | 新建主体 |
| merchantUserId | number | 新建登录账号 |
| phone | string | 申请联系电话，即商家登录号 |
| plainPassword | string | 明文，只此一次 |

`POST /api/admin/merchant-applies/{id}/reject`

| 字段 | 类型 | 必填 |
|------|------|------|
| reason | string | 是，最长 500 |

`data`：`id`、`status`（`REJECTED`）、`rejectReason`。

## 商家主体

`GET /api/admin/merchants`

查询：`status`（`ACTIVE` / `DISABLED`）、`page`、`pageSize`。

`items[]`：

| 字段 | 类型 |
|------|------|
| id | number |
| name | string |
| contactName | string \| null |
| contactPhone | string \| null |
| status | `ACTIVE` \| `DISABLED` |
| createdAt | string |
| users | array |

`users[]`：`id`、`phone`、`displayName`、`enabled`。

`PATCH /api/admin/merchants/{orgId}`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | `ACTIVE` \| `DISABLED` | 与 enabled 至少一个 | 主体状态 |
| enabled | boolean | 与 status 至少一个 | 该主体全部账号 |

`data` 与列表单项相同。

`POST /api/admin/merchants/{orgId}/reset-password`

| 字段 | 类型 | 必填 |
|------|------|------|
| password | string | 否，规则同入驻通过 |

`data`：

| 字段 | 类型 | 说明 |
|------|------|------|
| merchantOrgId | number | |
| merchantUserId | number | 该主体 id 最小的账号 |
| phone | string | 该账号手机号 |
| plainPassword | string | 主体下全部账号都改成这个密码，只返回一次 |

`POST /api/admin/merchants/{orgId}/stores` 见下一节「代建」。

## 门店

代建 `POST /api/admin/merchants/{orgId}/stores`。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 最长 128 |
| regionId | number | 是 | 须为启用地区 |
| address | string | 否 | 最长 256 |
| phone | string | 否 | 最长 32 |
| coverUrl | string | 否 | 最长 512 |
| gallery | string[] | 否 | 图集 URL |
| priceText | string | 否 | 最长 64，不是结算价 |
| priceFrom | number | 否 | 筛价格用 |
| packageText | string | 否 | 套餐权威文案 |
| facilities | string | 否 | |
| features | string | 否 | |
| notice | string | 否 | |
| trafficText | string | 否 | |
| shelfStatus | `DRAFT` \| `OFF_SHELF` \| `ON_SHELF` | 否 | 默认 `DRAFT` |
| homeRecommended | boolean | 否 | 默认 false |
| homeSort | number | 否 | 默认 100，越小越靠前，0–100000 |
| testData | boolean | 否 | 默认 false |

代编 `PUT /api/admin/stores/{id}`。这是代上下架的唯一入口：改 `shelfStatus` 即立即生效。没有 `POST /api/admin/stores/{id}/shelf`。

| 字段 | 类型 | 必填 |
|------|------|------|
| name | string | 是 |
| regionId | number | 是 |
| address | string | 否 |
| phone | string | 否 |
| coverUrl | string | 否 |
| gallery | string[] | 否。不传则保留原图集 |
| priceText | string | 否 |
| priceFrom | number | 否 |
| packageText | string | 否 |
| facilities | string | 否 |
| features | string | 否 |
| notice | string | 否 |
| trafficText | string | 否 |
| homeRecommended | boolean | 是 |
| homeSort | number | 是 |
| shelfStatus | `DRAFT` \| `OFF_SHELF` \| `ON_SHELF` | 是 |
| testData | boolean | 否。不传则保留 |
| version | number | 是 |

列表 `GET /api/admin/stores` 查询：`shelfStatus`、`regionId`、`merchantOrgId`、`homeRecommended`、`name`、`page`、`pageSize`。

详情 `GET /api/admin/stores/{id}` 与代建、代编的 `data` 相同：

| 字段 | 类型 |
|------|------|
| id | number |
| merchantOrgId | number |
| regionId | number |
| name | string |
| address | string \| null |
| phone | string \| null |
| coverUrl | string \| null |
| gallery | string[] |
| priceText | string \| null |
| priceFrom | number \| null |
| packageText | string \| null |
| facilities | string \| null |
| features | string \| null |
| notice | string \| null |
| trafficText | string \| null |
| shelfStatus | string |
| homeRecommended | boolean |
| homeSort | number |
| testData | boolean |
| version | number |

## 上下架申请

`GET /api/admin/shelf-applications`

查询：`status`、`storeId`、`merchantOrgId`、`page`、`pageSize`。

`items[]`：

| 字段 | 类型 |
|------|------|
| id | number |
| storeId | number |
| storeName | string \| null |
| merchantOrgId | number |
| action | `PUBLISH` \| `UNPUBLISH` |
| reason | string \| null |
| status | `PENDING` \| `APPROVED` \| `REJECTED` |
| rejectReason | string \| null |
| reviewedBy | number \| null |
| reviewedAt | string \| null |
| createdAt | string |

`POST /api/admin/shelf-applications/{id}/approve` 无请求体。`data` 与列表单项相同，`status` 为 `APPROVED`。

`POST /api/admin/shelf-applications/{id}/reject` 请求体只有必填 `reason`。`data.status` 为 `REJECTED`。门店 `shelfStatus` 不变。

通过后的门店状态：`PUBLISH` → `ON_SHELF`，`UNPUBLISH` → `OFF_SHELF`。

## 意向

`GET /api/admin/intents`

查询：

| 参数 | 类型 | 说明 |
|------|------|------|
| status | `PENDING` \| `CONFIRMED` \| `RESCHEDULED` \| `CHECKED_IN` \| `CLOSED` | |
| storeId | number | |
| merchantOrgId | number | |
| checkInFrom | date | 到店日起，含当天 |
| checkInTo | date | 到店日止，含当天 |
| createdFrom | date | 创建日起，含当天 |
| createdTo | date | 创建日止，含当天 |
| page | number | |
| pageSize | number | |

`items[]`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | |
| intentNo | string | 单号 |
| userId | number | 游客 id |
| storeId | number | |
| storeName | string \| null | |
| merchantOrgId | number | |
| checkIn | date | |
| checkOut | date \| null | |
| nights | number \| null | |
| guests | number | |
| contactName | string | |
| contactPhone | string | 完整号码 |
| userRemark | string \| null | |
| status | string | 英文码 |
| statusLabel | string | 中文。`CHECKED_IN` 为「已入住」 |
| version | number | |
| createdAt | string | |
| updatedAt | string | |

`GET /api/admin/intents/{id}` 的 `data` 在上述字段之外多一个 `notes` 数组。

`notes[]`：

| 字段 | 类型 |
|------|------|
| id | number |
| adminId | number |
| action | `STATUS_CHANGE` \| `COMMENT` |
| fromStatus | string \| null |
| toStatus | string \| null |
| note | string \| null |
| createdAt | string |

按 id 从早到晚。不删除旧备注。

`POST /api/admin/intents/{id}/status`

| 字段 | 类型 | 必填 |
|------|------|------|
| targetStatus | 意向状态码 | 是 |
| note | string | 否，最长 1000 |
| version | number | 是 |

合法迁移：

| 从 | 到 |
|----|----|
| PENDING | CONFIRMED、RESCHEDULED、CLOSED |
| CONFIRMED | CHECKED_IN、CLOSED |
| RESCHEDULED | CONFIRMED、CLOSED |

`CHECKED_IN` 和 `CLOSED` 不能再迁。`data` 是详情，含更新后的 `notes` 和 `version`。

`POST /api/admin/intents/{id}/notes`

| 字段 | 类型 | 必填 |
|------|------|------|
| note | string | 是，最长 1000 |

`GET /api/admin/intents/export` 使用和列表相同的筛选，不传分页。响应是 `text/csv;charset=UTF-8`，带 BOM，附件名 `intents.csv`。不是 `{code,message,data}`。

| 列 | 来源 |
|----|------|
| 单号 | intentNo |
| 门店 | storeName |
| 到店日期 | checkIn |
| 离店日期 | checkOut |
| 晚数 | nights |
| 人数 | guests |
| 状态 | statusLabel（已入住，不是已达成/已落地/已落实/已成交） |
| 联系人 | contactName |
| 手机号 | 完整 contactPhone |
| 游客备注 | userRemark |

没有佣金、金额、支付列。

中文对照：待跟进、已确认、已改期、已入住、已关闭。`CHECKED_IN` 是「已入住」，不是已达成/已落地/已落实/已成交。

## 攻略

`GET /api/admin/articles` 查询：`status`（`DRAFT` / `PUBLISHED`）、`page`、`pageSize`。含草稿。

`POST /api/admin/articles`、`PUT /api/admin/articles/{id}`：

| 字段 | 类型 | 必填 |
|------|------|------|
| title | string | 是，最长 200 |
| coverUrl | string | 否，最长 512 |
| summary | string | 否，最长 500 |
| body | string | 否 |
| status | `DRAFT` \| `PUBLISHED` | 是 |
| regionId | number | 否 |

`data` 与 `GET /api/admin/articles/{id}`：

| 字段 | 类型 |
|------|------|
| id | number |
| title | string |
| coverUrl | string \| null |
| summary | string \| null |
| body | string \| null |
| status | string |
| regionId | number \| null |
| publishedAt | string \| null |
| createdBy | number |
| createdAt | string |
| updatedAt | string |

`DELETE /api/admin/articles/{id}` 的 `data` 为 `{ "id": number }`。

`PUBLISHED` 后门户 `GET /api/articles` 和 `GET /api/articles/{id}` 可见。草稿对门户是 404 或不在列表中。

## 开城

开通单位是市。`enabled` 仍表示节点可维护，`openStatus` 才表示对客开通。省和区县没有开通态。没有支付，没有强制开通。

`GET /api/admin/cities` 不分页。只含 `enabled=true` 且层级为市的节点，按 `sortOrder, id`。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 市 id |
| provinceName | string \| null | 父省名称。父节点不是省则为 null |
| name | string | 如扬州市 |
| slug | string | 如 `yangzhou` |
| openStatus | `OPEN` \| `CLOSED` | 库内空值也返回 `CLOSED` |
| storeCount | number | 上架、非测试、地区启用，且挂在该市或下属区县 |
| articleCount | number | 已发布，且挂在该市或下属区县 |
| updatedAt | string \| null | 清单最近操作时间。没有清单行则为 null |

`GET /api/admin/cities/{cityId}` 在上面的字段之外还有：

| 字段 | 类型 | 说明 |
|------|------|------|
| enabled | boolean | 停用的市返回 400「地区不可用」 |
| minStores | number | 生效的最低上架店数，默认 5 |
| minArticles | number | 生效的最低攻略数，默认 1 |
| checklist.destinationReady | boolean | ①目的地数据齐，手勾 |
| checklist.supplyReady | boolean | ②按 `storeCount >= minStores` 推导，不落库 |
| checklist.articlesReady | boolean | ③按 `articleCount >= minArticles` 推导，不落库 |
| checklist.supportCovered | boolean | ④客服覆盖，手勾 |
| checklist.dirtyDataCleared | boolean | ⑤脏数据已清理，手勾 |
| canOpen | boolean | 当前不是 OPEN，且三项手勾和两项计数都达标 |
| canClose | boolean | 当前是 OPEN |
| missingReasons | string[] | 未满足项。已经开通且只是「已经开了」时为空数组 |
| version | number | 没有清单行时为 0 |
| updatedBy | number \| null | 最近操作的运营 id |

不是市、或不存在：404「城市不存在」。

`PATCH /api/admin/cities/{cityId}/checklist`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| destinationReady | boolean | 否 | 省略表示不改 |
| supportCovered | boolean | 否 | 省略表示不改 |
| dirtyDataCleared | boolean | 否 | 省略表示不改 |
| version | number | 是 | 乐观锁 |

三项至少要改一项，否则 400「请指定要更新的项」。请求体若带 `supplyReady` 或 `articlesReady`，400「供给达标与攻略达标按实时计数自动勾选，不能手改」，不改版本。成功 `data` 与详情相同。手勾不刷新门户缓存。

`POST /api/admin/cities/{cityId}/open` 和 `POST /api/admin/cities/{cityId}/close` 的请求体只有必填 `version`。

| HTTP | message |
|------|---------|
| 409 | 该市已开通 |
| 409 | 该市未开通 |
| 409 | 未达标，不能开通：再加上缺项，用中文分号连接 |
| 409 | 数据已被修改 |

缺项顺序固定：目的地数据未齐；供给未达标（当前 {storeCount} / 需要 {minStores}）；攻略未达标（当前 {articleCount} / 需要 {minArticles}）；客服覆盖未说明；测数或脏数据未确认清理。

开通和关城成功后失效地区缓存和首页缓存。关城不删意向、门店、攻略，也不改 `enabled`。计数后来跌破 N/M 不会自动关城。

`GET /api/admin/metrics/city-intents`

| 参数 | 说明 |
|------|------|
| window | `7`、`30`、`ALL`。省略默认 `7`。其他值 400「参数不正确」 |
| cityId | 可选。必须是启用的市，否则 404「城市不存在」 |

时间打在 `booking_intent.created_at`。`7` / `30` 是从现在往前滚动的 7 或 30 天，不是自然日零点。`data`：

| 字段 | 说明 |
|------|------|
| window | 实际使用的 `7` / `30` / `ALL` |
| from | 下界。`ALL` 为 null |
| rows | 第一行是合计，其后是市 |

行字段：`cityId`（合计为 null）、`cityName`（合计为「合计」）、`intentCount`、`confirmedCount`（只数当前 `CONFIRMED`）、`checkedInCount`（只数当前 `CHECKED_IN`，文案仍是已入住）、`total`（合计为 true）。没有意向 id、门店 id 或跳转字段。`RESCHEDULED` 计入意向数，不计入已确认。

`GET /api/admin/metrics/city-intents/export` 使用相同查询。响应是 `text/csv;charset=UTF-8`，带 BOM，附件名 `city-intents.csv`。列：`市,意向数,已确认,已入住`。第一数据行是合计。不是 `{code,message,data}`。

门户：`GET /api/regions` 只留下已开通的市和它们的区县。`GET /api/stores` 的 `data` 在 `items,total,page,pageSize` 之外增加 `destinationStatus`（`OPEN` / `CLOSED` / `NONE`）和 `emptyHint`。未开通市的详情和创建意向复用 410「已下架或不可用」。

## 站点配置

`GET /api/admin/site-config` 与 `PUT /api/admin/site-config` 的 `data`：

| 字段 | 类型 | 说明 |
|------|------|------|
| siteName | string | 对应 `site.name` |
| brandColor | string | 对应 `site.brand_color`，如 `#4BA3D9` |
| contactPhone | string | 对应 `site.contact_phone` |

PUT 三个字段都必填。`brandColor` 必须匹配 `^#[0-9A-Fa-f]{6}$`。`siteName` 最长 64，`contactPhone` 最长 32。

种子默认值：站名「站点名称」，主色 `#4BA3D9`，电话 `400-000-0000`。门户读 `GET /api/config/public`，字段名与上表相同。

## 首页 Banner

运营配置门户首页 Banner。`OPERATOR` 与 `AGENT` 都可以。图片只收 `http` / `https` 地址，没有上传接口。不挂在站点配置上。无支付、无买位、无曝光统计。

详情、创建、更新、启停共用这些字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | |
| title | string \| null | 运营标题。空白存 null |
| imageUrl | string | 最长 512，须 `http://` 或 `https://` |
| linkUrl | string | 最长 512。站内以单个 `/` 开头，或 `http(s)` 外链 |
| scope | `ALL` \| `CITY` | 全站或指定市 |
| cityId | number \| null | `ALL` 为 null |
| citySlug | string \| null | 市的 `region.slug`，如 `yangzhou`。没有 `cityCode` |
| cityName | string \| null | 如扬州市。`ALL` 为 null |
| cityOpen | boolean \| null | `CITY` 且该市当前开通为 true，否则 `CITY` 为 false。`ALL` 为 null |
| sortOrder | number | 0～1000000，越小越靠前。同值再按 id 升序 |
| enabled | boolean | |
| createdAt / updatedAt | string | 上海时区 ISO-8601 |

`GET /api/admin/banners` 分页，固定 `sortOrder` 升序、`id` 升序。空列表 `items=[]`。

| 参数 | 说明 |
|------|------|
| enabled | `true` / `false`。省略表示都要 |
| scope | `ALL` / `CITY`。其他值 400「参数不正确」 |
| keyword | 匹配标题或跳转链接，先 trim。空串当省略。超过 64 字 400「参数不正确」 |
| page / pageSize | 与现网相同。非法 400「分页参数不正确」 |

`POST /api/admin/banners`：`imageUrl`、`linkUrl`、`scope` 必填。`title` 可空。`cityId` 在 `CITY` 时必填，且必须是已开通的市。`sortOrder` 省略为 100。`enabled` 省略为 true。成功 `data` 为详情。

`PUT /api/admin/banners/{id}` 全量更新。`imageUrl`、`linkUrl`、`scope`、`sortOrder`、`enabled` 必填。`title`、`cityId` 可空。不存在 404「Banner 不存在」。指定市后来被关成 CLOSED 时，保存前必须改成全站或另一座已开通的市。

`POST /api/admin/banners/{id}/enable` 与 `POST /api/admin/banners/{id}/disable` 无请求体。已经是目标状态返回当前详情，不重复写审计。启用一条 `CITY` 且该市现已不是开通：409「投放市未开通，请改选全站或其他已开通市」。停用不受市开通态限制。关城不改 `enabled`。

`DELETE /api/admin/banners/{id}` 物理删除。成功 `data` 为 `{ "id": number }`。

写入按顺序命中即返回：没填图片「请填写图片地址」；图片超长「图片地址过长」；图片不是 http(s)「图片地址须为 http 或 https」；没填链接「请填写跳转链接」；链接超长「跳转链接过长」；链接形态不对「跳转链接须为站内路径或 http(s) 外链」；标题超长「标题过长」；排序非法「请输入整数排序值」；没选范围「请选择投放范围」；全站却带了市「全站投放不能指定市」；指定市却没选「请选择已开通的市」；市不存在或不是市 404「城市不存在」；市节点停用「地区不可用」；市未开通「投放市未开通，请改选全站或其他已开通市」；更新没带 enabled「请指定是否启用」。未知字段、坏 JSON、非法 scope 码为「请求体格式不正确」。路径 id 不是数字为「参数不正确」。

门户 `GET /api/home/banners` 公开，无查询参数。多出来的查询串忽略，不按游客当前市过滤。`data` 是数组，不是分页。元素不含 `cityOpen` 和时间。`enabled` 恒为 true。入集：启用，且（全站，或指定市且该市当前仍开通），再按排序升序。0 条为 `[]`。不走 Redis，也不进 `GET /api/home/feed`。

种子：全站「苏中逛一逛」排序 10，链接 `/`；扬州「扬州周末」排序 20，链接 `/?city=yangzhou`。图片是 `https://example.com/njl/banner-all.png` 与 `banner-yangzhou.png`。不改三市 OPEN。
