# 后端接口索引

> 由源码扫描生成（26 个 Controller，69 个路由），基准地址 `http://localhost:8080`。
> 除 `/health` 外统一信封 `{code, message, data}`，`code = 0` 表示成功。字段细节见 `docs/ADMIN-API.md`。
> 在线版：本地启动后访问 `/swagger-ui.html`（OpenAPI JSON 为 `/v3/api-docs`），由源码注解生成，与本文档同源。

## 通用约定

| 项 | 说明 |
|----|------|
| 鉴权 | `Authorization: Bearer <token>`，令牌 32 字节随机串存进程内存，默认 12 小时，重启失效 |
| 角色 | `TOURIST`（游客）、`MERCHANT`（商家）、`OPERATOR` / `AGENT`（运营） |
| 分页 | `page` 从 1 开始，`pageSize` 由服务端限幅 |
| 错误 | 401 未登录；403 角色不符或账号停用；404 不存在；409 冲突或乐观锁；410 下架/未开通 |
| 未知路径 | 含 `/api/payments`，返回 HTTP 404 + `{code:404,message:"接口不存在"}` |
| CORS | 仅 `43123` / `43124` / `43125` |

## 一、公共与门户（20）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 1 | GET | `/health` | 公开 | 探针，不走信封，返回 `{status,service}` |
| 2 | POST | `/api/auth/register` | 公开 | 游客注册，`phone` / `password` / `displayName` |
| 3 | POST | `/api/auth/login` | 公开 | 游客登录，`phone` / `password` |
| 4 | GET | `/api/auth/me` | TOURIST | 当前游客 |
| 5 | POST | `/api/auth/wechat/login` | 公开 | `code` 换游客令牌；未配置微信返回 503 |
| 6 | POST | `/api/auth/wechat/bind` | TOURIST | 绑手机号：`phoneCode`，或 `phone` + `password` |
| 7 | POST | `/api/auth/wechat/unbind` | TOURIST | 解绑当前 AppID 的 openid |
| 8 | GET | `/api/auth/wechat/link` | TOURIST | 读取打通状态 |
| 9 | GET | `/api/config/public` | 公开 | 站名、主色、客服电话 |
| 10 | GET | `/api/regions` | 公开 | 已开通市 + 其下启用区县 |
| 11 | GET | `/api/home/feed` | 公开 | 首页推荐门店与攻略 |
| 12 | GET | `/api/home/banners` | 公开 | 启用 Banner，按 `sortOrder` 升序，无城市参数 |
| 13 | GET | `/api/stores` | 公开 | 房源列表：`regionId` `tag` `priceMin` `priceMax` `sort` `page` `pageSize` `keyword` |
| 14 | GET | `/api/stores/{id}` | 公开 | 房源详情；下架或未开通市 410 |
| 15 | GET | `/api/articles` | 公开 | 已发布攻略，`page` / `pageSize` |
| 16 | GET | `/api/articles/{id}` | 公开 | 攻略详情，直链可开 |
| 17 | POST | `/api/booking-intents` | TOURIST | 提交意向（无支付、无锁房） |
| 18 | GET | `/api/me/intents` | TOURIST | 我的意向，`page` / `pageSize` |
| 19 | GET | `/api/me/intents/{id}` | TOURIST | 意向详情，非本人 404 |
| 20 | POST | `/api/merchant-applies` | 公开 | 提交入驻申请 |

`sort` 仅接受 `comprehensive` / `price_asc` / `price_desc`；`keyword` 最长 64 字，超长 400。

## 二、商家端（8）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 21 | POST | `/api/merchant/auth/login` | 公开 | `phone` / `password`；停用 403 |
| 22 | GET | `/api/merchant/stores` | MERCHANT | 本主体门店（含下架），`page` / `pageSize` |
| 23 | GET | `/api/merchant/stores/{id}` | MERCHANT | 门店详情 |
| 24 | PUT | `/api/merchant/stores/{id}` | MERCHANT | 编辑资料，白名单字段 + `version`；不能改上下架与推荐 |
| 25 | POST | `/api/merchant/stores/{id}/shelf-applications` | MERCHANT | 申请上架/下架，`action` = `PUBLISH` / `UNPUBLISH` |
| 26 | GET | `/api/merchant/shelf-applications` | MERCHANT | 本主体申请记录 |
| 27 | GET | `/api/merchant/intents` | MERCHANT | 本店意向，只读，手机号脱敏 |
| 28 | GET | `/api/merchant/intents/{id}` | MERCHANT | 意向详情，只读 |

商家端没有建店接口，也没有 `GET /api/merchant/auth/me`。

## 三、运营端（41）

### 3.1 登录

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 29 | POST | `/api/admin/auth/login` | 公开 | `phone` / `password` |
| 30 | GET | `/api/admin/auth/me` | OPERATOR / AGENT | 当前运营身份 |

### 3.2 入驻审核

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 31 | GET | `/api/admin/merchant-applies` | 运营 | 队列，`status` = `PENDING` / `APPROVED` / `REJECTED` |
| 32 | POST | `/api/admin/merchant-applies/{id}/approve` | 运营 | 通过并开通账号，可选 `password`（8–64 位），明文只返回一次 |
| 33 | POST | `/api/admin/merchant-applies/{id}/reject` | 运营 | 驳回，`reason` 必填 |

### 3.3 商家主体

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 34 | GET | `/api/admin/merchants` | 运营 | 主体列表，`status` = `ACTIVE` / `DISABLED` |
| 35 | PATCH | `/api/admin/merchants/{orgId}` | 运营 | 改 `status` 或账号 `enabled`；停用后旧令牌失效 |
| 36 | POST | `/api/admin/merchants/{orgId}/reset-password` | 运营 | 重置密码，可选 `password`，明文只返回一次 |
| 37 | POST | `/api/admin/merchants/{orgId}/stores` | 运营 | 代建门店并绑定主体 |

### 3.4 门店

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 38 | GET | `/api/admin/stores` | 运营 | 全站门店，`shelfStatus` `regionId` `merchantOrgId` `homeRecommended` `name` |
| 39 | GET | `/api/admin/stores/{id}` | 运营 | 门店详情 |
| 40 | PUT | `/api/admin/stores/{id}` | 运营 | 代编整单保存；改 `shelfStatus` 即代上下架，写 `SHELF_FORCE` 审计 |

### 3.5 上下架申请

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 41 | GET | `/api/admin/shelf-applications` | 运营 | 队列，`status` `storeId` `merchantOrgId` |
| 42 | POST | `/api/admin/shelf-applications/{id}/approve` | 运营 | 通过并改门店上下架状态 |
| 43 | POST | `/api/admin/shelf-applications/{id}/reject` | 运营 | 驳回，`reason` 必填 |

### 3.6 意向

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 44 | GET | `/api/admin/intents` | 运营 | 筛选：`status` `storeId` `merchantOrgId` `checkInFrom/To` `createdFrom/To` |
| 45 | GET | `/api/admin/intents/export` | 运营 | 同筛选条件导出 CSV（UTF-8 BOM，无佣金列） |
| 46 | GET | `/api/admin/intents/{id}` | 运营 | 详情，完整手机号 + 备注时间线 |
| 47 | POST | `/api/admin/intents/{id}/status` | 运营 | 状态机迁移：`targetStatus` `note` `version` |
| 48 | POST | `/api/admin/intents/{id}/notes` | 运营 | 只追加备注：`note` |

`IntentStatus`：`PENDING` 待跟进、`CONFIRMED` 已确认、`RESCHEDULED` 已改期、`CHECKED_IN` 已入住、`CLOSED` 已关闭。

### 3.7 攻略

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 49 | GET | `/api/admin/articles` | 运营 | 列表，`status` = `DRAFT` / `PUBLISHED` |
| 50 | POST | `/api/admin/articles` | 运营 | 新建 |
| 51 | GET | `/api/admin/articles/{id}` | 运营 | 详情 |
| 52 | PUT | `/api/admin/articles/{id}` | 运营 | 全量更新 |
| 53 | DELETE | `/api/admin/articles/{id}` | 运营 | 删除 |

### 3.8 站点配置

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 54 | GET | `/api/admin/site-config` | 运营 | 当前配置 |
| 55 | PUT | `/api/admin/site-config` | 运营 | `siteName` `brandColor` `contactPhone`；不含开城门槛 |

### 3.9 首页 Banner

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 56 | GET | `/api/admin/banners` | 运营 | `enabled` `scope` `keyword` |
| 57 | POST | `/api/admin/banners` | 运营 | 新建 |
| 58 | GET | `/api/admin/banners/{id}` | 运营 | 详情 |
| 59 | PUT | `/api/admin/banners/{id}` | 运营 | 全量更新 |
| 60 | POST | `/api/admin/banners/{id}/enable` | 运营 | 启用；投放市已关闭返回 409 |
| 61 | POST | `/api/admin/banners/{id}/disable` | 运营 | 停用 |
| 62 | DELETE | `/api/admin/banners/{id}` | 运营 | 物理删除 |

`BannerScope`：`ALL` 全站、`CITY` 指定市。图片只收 URL。

### 3.10 开城

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 63 | GET | `/api/admin/cities` | 运营 | 开城列表 |
| 64 | GET | `/api/admin/cities/{cityId}` | 运营 | 详情，供给/攻略为实时计数 |
| 65 | PATCH | `/api/admin/cities/{cityId}/checklist` | 运营 | 只手改 3 项 + `version`；带 `supplyReady`/`articlesReady` 返回 400 |
| 66 | POST | `/api/admin/cities/{cityId}/open` | 运营 | 达标才开通，未达标 409 |
| 67 | POST | `/api/admin/cities/{cityId}/close` | 运营 | 关城，不删历史 |

### 3.11 种子看板

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|------|------|------|------|
| 68 | GET | `/api/admin/metrics/city-intents` | 运营 | `window` = `7` / `30` / `ALL`，可选 `cityId` |
| 69 | GET | `/api/admin/metrics/city-intents/export` | 运营 | 同筛选导出 CSV |

## 四、请求体速查

| 接口 | 字段 |
|------|------|
| `POST /api/booking-intents` | `storeId` `checkIn` `checkOut` `nights` `guests` `contactName` `contactPhone` `userRemark` |
| `POST /api/merchant-applies` | `orgName` `contactName` `contactPhone` `plannedStoreName` `regionId` `description` |
| `PUT /api/merchant/stores/{id}` | `name` `regionId` `address` `phone` `coverUrl` `gallery` `priceText` `priceFrom` `packageText` `facilities` `features` `notice` `trafficText` `version` |
| `POST .../shelf-applications` | `action`（`PUBLISH` / `UNPUBLISH`）`reason` |
| `PUT /api/admin/stores/{id}` | 商家字段 + `homeRecommended` `homeSort` `shelfStatus` `testData` `version` |
| `POST /api/admin/merchants/{orgId}/stores` | 门店字段 + `shelfStatus` `homeRecommended` `homeSort` `testData`（缺省 `DRAFT` / `false` / `100`） |
| `POST /api/admin/intents/{id}/status` | `targetStatus` `note` `version` |
| `POST /api/admin/articles` | `title` `coverUrl` `summary` `body` `status` `regionId` |
| `POST /api/admin/banners` | `title` `imageUrl` `linkUrl` `scope` `cityId` `sortOrder` `enabled` |
| `PATCH /api/admin/cities/{id}/checklist` | `destinationReady` `supportCovered` `dirtyDataCleared` `version` |

所有请求体 `@JsonIgnoreProperties(ignoreUnknown = false)`：多传未知字段直接拒绝。

## 五、尚未实现

| 项 | 状态 |
|----|------|
| 品牌站点配置四字段（`siteNameEn` `slogan` `logoUrl` `faviconUrl`） | 技术设计已定，代码未写；当前 `site-config` 仍只有三字段 |
| 支付、定金、锁房、佣金 | 明确不做，路径返回 404 |
| 商家建店、商家 `auth/me` | 无，设计如此 |
| 图片上传 | 无，图片一律只收 URL |
| 令牌持久化 | 内存存储，重启失效 |
