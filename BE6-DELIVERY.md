# BE-6 运营 API 交付

仓库：`andrew-yang/nongjiale-api`。在已合并的门户（BE-4）和商家（BE-5）上补齐 `/api/admin/**` 业务接口。没有支付、锁房和佣金。

前端接线以 `docs/ADMIN-API.md` 的字段名为准。`homeRecommended`、`homeSort` 不要写成 `homeRecommendSort`。

## ① 概要设计

运营登录仍是 `POST /api/admin/auth/login`（手机号 + 密码）。本任务补审入驻、商家启停与代建门店、房源代编和首页推荐、代上下架、上下架申请审核、意向跟进与导出、攻略和站点配置。

令牌角色是 `OPERATOR` 或 `AGENT`（权限相同）。游客、商家令牌访问运营接口返回 403，未登录返回 401。响应信封仍是 `{code, message, data}`。列表是 `{items, total, page, pageSize}`，`page` 从 1 起。

状态和动作继续用已有枚举：`ReviewStatus`、`MerchantOrgStatus`、`ShelfStatus`、`ShelfAction`、`IntentStatus`、`NoteAction`、`ArticleStatus`、`SiteConfigKey`、`AuditAction`。审计目标类型新增 `AuditTarget`，只写入 `audit_log.target_type`。

## ② 详细设计

### 入驻审核

`GET /api/admin/merchant-applies` 可按 `status=PENDING|APPROVED|REJECTED` 筛选。

通过时创建 `merchant_org`（`ACTIVE`）和 `merchant_user`（手机号等于申请联系电话，`enabled=true`）。请求可带 `password`（8 到 64 位）；不传则生成 10 位临时密码。明文只出现在当次响应的 `plainPassword`，库里是 BCrypt。申请改为 `APPROVED`，回填 `merchantOrgId`，并写 `APPLY_APPROVE`。

驳回时 `reason` 必填，申请改为 `REJECTED`，不创建账号，写 `APPLY_REJECT`。不是 `PENDING` 再审返回 409「申请已审核」。

通过后不自动建门店。门店走代建接口。

### 商家主体

`PATCH /api/admin/merchants/{orgId}` 的 `status` 改主体，`enabled` 改该主体下全部登录账号。至少传一个。

- 主体 `DISABLED`：再次登录 403「商家已停用」
- 账号 `enabled=false`：再次登录 403「账号已停用」

停用会丢掉该主体尚未过期的内存令牌，旧 Bearer 再访问商家接口是 401。启用写 `MERCHANT_ENABLE`，停用写 `MERCHANT_DISABLE`。

`POST /api/admin/merchants/{orgId}/reset-password` 把该主体全部账号改成同一个新密码，响应只给一次 `plainPassword`，并吊销旧令牌。密码策略与开通相同。

`POST /api/admin/merchants/{orgId}/stores` 代建门店并写入 `merchantOrgId`。商家端仍然没有创建门店接口。未传 `shelfStatus` 时为 `DRAFT`，未传 `homeRecommended` 时为 false，未传 `homeSort` 时为 100。

### 房源代编、推荐和代上下架

`GET /api/admin/stores` 可按 `shelfStatus`、`regionId`、`merchantOrgId`、`homeRecommended`、`name` 筛选。

`PUT /api/admin/stores/{id}` 整单保存文案、地址、`homeRecommended`、`homeSort` 和 `shelfStatus`。必须带当前 `version`，不一致返回 409「数据已被修改」。

代上下架不另开接口。`shelfStatus` 与当前值不同时立即改 `store.shelf_status`，写 `audit_log.action=SHELF_FORCE`，不修改 `shelf_application`。写成功后失效门店详情和首页缓存。门户下架详情仍是 410「已下架或不可用」，本任务不改这条行为。

### 上下架申请

`GET /api/admin/shelf-applications` 可筛 `status`、`storeId`、`merchantOrgId`。

通过：`PUBLISH` 把门店改为 `ON_SHELF`，`UNPUBLISH` 改为 `OFF_SHELF`，申请改为 `APPROVED`，写 `SHELF_REVIEW`，并失效缓存。驳回：`reason` 必填，申请 `REJECTED`，门店上架状态不变。不是 `PENDING` 返回 409。

### 意向

列表和导出共用筛选：`status`、`storeId`、`merchantOrgId`、`checkInFrom`、`checkInTo`、`createdFrom`、`createdTo`。

详情带备注时间线，`contactPhone` 是完整号码。改状态的请求体是 `targetStatus`、`version`，可选 `note`。只允许 `IntentStatus.canTransitTo`。非法迁移 409，文案形如「不能从待跟进改为已落地」。版本不一致 409「数据已被修改」。每次改状态追加一条 `STATUS_CHANGE` 备注，不删历史，并写 `INTENT_STATUS`。`POST .../notes` 只追加 `COMMENT`。

`GET /api/admin/intents/export` 返回 CSV，不是 JSON 信封。表头为：单号、门店、到店日期、离店日期、晚数、人数、状态、联系人、手机号、游客备注。状态用枚举中文，`CHECKED_IN` 为「已落地」。没有佣金列。筛选与列表一致，最多 10000 行。

### 攻略和站点配置

攻略 CRUD 在 `/api/admin/articles`。`PUBLISHED` 后 `GET /api/articles` 可见，草稿不可见。首次发布写入 `publishedAt`。写后失效首页缓存（首页含攻略摘要）。门户列表的计数不再把 `ORDER BY` 带进 `COUNT`，避免草稿 `published_at` 为空时 H2 报错。

`GET/PUT /api/admin/site-config` 的字段是 `siteName`、`brandColor`、`contactPhone`，对应 `SiteConfigKey`。主色须为 `#` 加 6 位十六进制。写入后失效 `site-config` 缓存，并写 `SITE_CONFIG_UPDATE`。种子主色仍是 `#4BA3D9`，本任务不改 Flyway。

## ③ 改动点

| 对象 | 动作 |
|------|------|
| `web.admin` / `service.admin` | 入驻审核、商家、门店、上下架审核、意向、攻略、站点配置 |
| `TokenStore.revokeMerchantOrg` | 停用或改密时丢掉该主体商家令牌 |
| `AuditTarget` | 审计目标类型 |
| `ArticleQueryService` | 门户攻略计数去掉排序，避免空 `published_at` 让 H2 失败 |
| `AdminApiIntegrationTest` | 通过/驳回、状态机合法与非法、导出含已确认和已落地、`homeRecommended` 可写、商家令牌调运营 403 |
| `README.md`、`docs/ADMIN-API.md` | 运营路径和 DTO 字段 |

没有新的 Flyway 版本。

## ④ 影响点

- 运营前端按 `docs/ADMIN-API.md` 接线即可。代上下架只走门店 `PUT` 的 `shelfStatus`。
- 商家停用文案与 BE-5 一致。旧令牌在停用或改密后失效，需要重新登录。
- 门户 410、商家隔离、推荐字段商家不可写，这些行为保持不变。
- 开通和重置密码的明文只在当次响应里，后续查询不再返回。
- 意向导出是 CSV 附件。`CHECKED_IN` 列值是「已落地」。
- 默认主色种子仍是 `#4BA3D9`。改配置后再读门户公开配置即可看到新值。
