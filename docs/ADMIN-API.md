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
| statusLabel | string | 中文。`CHECKED_IN` 为「已达成」 |
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
| 状态 | statusLabel（已达成，不是已落地/已成交/已落实） |
| 联系人 | contactName |
| 手机号 | 完整 contactPhone |
| 游客备注 | userRemark |

没有佣金、金额、支付列。

中文对照：待跟进、已确认、已改期、已达成、已关闭。`CHECKED_IN` 是「已达成」，不是已落地/已成交/已落实。

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

## 站点配置

`GET /api/admin/site-config` 与 `PUT /api/admin/site-config` 的 `data`：

| 字段 | 类型 | 说明 |
|------|------|------|
| siteName | string | 对应 `site.name` |
| brandColor | string | 对应 `site.brand_color`，如 `#4BA3D9` |
| contactPhone | string | 对应 `site.contact_phone` |

PUT 三个字段都必填。`brandColor` 必须匹配 `^#[0-9A-Fa-f]{6}$`。`siteName` 最长 64，`contactPhone` 最长 32。

种子默认值：站名「站点名称」，主色 `#4BA3D9`，电话 `400-000-0000`。门户读 `GET /api/config/public`，字段名与上表相同。
