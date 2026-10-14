# 农家乐 API / 数据说明

> **Demo 基线（B1）已实现，下文「Demo 基线」仍然有效。**  
> **印程正式版 v1.1（商家第三端）只在本文定契约，本增量不实现 Java、不改 Flyway。**  
> 更新：2026-10-14

对齐：

- 范围冻结：[PRODUCT-INCHENG-v1.1.md](./PRODUCT-INCHENG-v1.1.md)（合并前见 [PR #1](https://github.com/yangchuntao/nongjiale-api/pull/1)，分支 `cursor/incheng-prd-draft-18a3`）
- 三端设计：[DESIGN.md](./DESIGN.md)、[UI-PAGES.md](./UI-PAGES.md)（合并前见 [PR #2](https://github.com/yangchuntao/nongjiale-api/pull/2)，分支 `cursor/design-docs-incheng-v1-1-2486`）
- 前端工程名：`apps/portal` · `apps/admin` · `apps/merchant`

## 栈

- Java 17（本地可用 JDK 21 编译）· Spring Boot 3.3 · Web · Validation · JPA · Flyway · Security
- 本地默认 H2 内存库；生产可切 MySQL（改 `application.yml`）

## 迁移

| 脚本 | 内容 | 状态 |
|------|------|------|
| `V1__init_schema.sql` | 用户、管理端、地区、房源、房型、评价、资讯、推荐位、意向单、备注、留言 | Demo 已有 |
| `V2__seed_yangtze_delta.sql` | 长三角地区 + 演示账号 + 样例房源 | Demo 已有 |

v1.1 需要的商家主体、入驻申请、上下架申请、标签、站点配置等表 **不在本文件对应的 PR 里加迁移**。实现另开 PR。

演示账号（仅开发/测试）：游客 `13800000001` / `password`；运营 `operator` / 客服 `agent` / `password`。商家 A/B 账号随 v1.1 实现再种子，本文不新增。

## 统一约定

- 响应：`{ "code": 0, "message": "ok", "data": ... }`；失败时 `data` 为 `null`，`code` 与 HTTP 状态对齐
- 列表：`data: { "items": [], "total": 0 }`；分页 `page` 从 1 起，`pageSize` 默认 20、最大 50（门户房源列表 Demo 默认 10）
- 鉴权：`Authorization: Bearer <token>`。Demo 为一期内存 Session；v1.1 继续用不透明 token，**逻辑 claim 必须能还原**（日后换 JWT 时字段名保持不变）
- 日期：`YYYY-MM-DD`；时间戳 ISO-8601
- 乐观锁：带 `version` 的更新，不一致时 **409**，`message` 提示刷新

### 角色 claim

未带 token 的调用方视为 **GUEST**（无 claim）。

| 端 | `type` | `role` | `merchantId` |
|----|--------|--------|----------------|
| 门户已登录用户 | `TOURIST` | `USER` | 无 |
| 运营 | `ADMIN` | `OPERATOR` | 无 |
| 客服 | `ADMIN` | `AGENT` | 无 |
| 商家 | `MERCHANT` | `MERCHANT` | **必填**，来自服务端绑定 |

Demo 已签发的游客 token 里 `role` 字面量是 `TOURIST`。v1.1 把 `TOURIST` 与 `USER` 视为同一门户用户。新的门户登录响应 **增加** `"role":"USER"`，不删除既有字段。

商家接口上的 `merchantId` **只信 token**。查询串或 body 里的 `merchantId` 若与 token 不一致 → **403**；缺省则忽略，仍按 token 过滤。

### 错误

| HTTP | `code` | 何时 |
|------|--------|------|
| 401 | 401 | 未登录、token 无效或过期、账号停用后的登录失败 |
| 403 | 403 | 已登录但角色不够；商家改意向/写跟进；客服做入驻审、上下架审、站点配置、商家账号 |
| 400 | 400 | 字段校验失败（信息与 Demo 一样，可多条用 `; ` 拼接） |
| 404 | 404 | 资源不存在；**或资源存在但不属于当前商家**（不暴露他人数据） |
| 409 | 409 | `version` 冲突、重复待审、手机号/登录名冲突 |
| 410 | 410 | 门户房源详情不可订（下架或尚未上架生效） |
| 422 | 422 | 调用方有权访问该资源，但当前状态不允许（已上架再申请上架、上架中删除门店等） |

未登录与无权限的 body 与现实现一致：

```json
{ "code": 401, "message": "未登录", "data": null }
{ "code": 403, "message": "无权限", "data": null }
```

商家尝试改意向状态或写跟进：**403**（能力拒绝）。前端把 **403 与 422 都当成失败**，不要乐观更新；状态与备注保持不变。

---

## Demo 基线（已实现，仍有效）

对齐当时的 P1：必须登录才能提交意向 · 无支付 · 无锁房。`contactPhone` 默认 `13651814168`（配置项 `nongjiale.contact-phone`）。

**Demo 当时明确不做、v1.1 已改口的只有一件：** 只读「我的意向」。见下文门户增量。支付、锁房、库存、积分仍然不做。

**Demo-only（v1.1 不扩展成产品能力）：** 首页 `topics` 专题频道保持空数组，不新增专题接口。

### 门户

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | GUEST | 注册并发 token。body：`phone`,`password`,`displayName`。409 手机号已注册 |
| POST | `/api/auth/login` | GUEST | body：`phone`,`password`。失败 401「手机号或密码错误」 |
| GET | `/api/auth/me` | 门户用户 | 当前用户 |
| GET | `/api/config/public` | GUEST | Demo 仅 `contactPhone`。v1.1 起增加字段，见站点配置 |
| GET | `/api/regions` | GUEST | 启用地区；`data` 为**纯数组**（非 `{items,total}`） |
| GET | `/api/home/feed` | GUEST | `regions`,`hotHomestays`,`topics`(空),`articles` |
| GET | `/api/homestays` | GUEST | 仅已上架。`regionId`/`regionSlug`/`keyword`/`priceMin`/`priceMax`/`page`/`pageSize`。门户 URL 的 `q` 由前端映射为 `keyword` |
| GET | `/api/homestays/{id}` | GUEST | 详情；不可订 **410**「该农家乐暂不可预订」 |
| POST | `/api/booking-intents` | 门户用户 | 提交意向；幂等键 `userId:homestayId:checkIn` |
| POST | `/api/contact-messages` | 门户用户 | 咨询留言 |

登录/注册成功 `data`：`token`,`userId`,`phone`,`displayName`。v1.1 额外加 `role:"USER"`。

提交意向 body：`homestayId`,`checkIn`,`checkOut`,`guests`(≥1),`travelMode?`,`contactName`,`contactPhone`。  
成功 `data`：`intentId`,`intentNo`,`status`,`contactPhone`（站点客服电话，不是游客填写的手机）。  
同用户、同房源、同到店日再次提交：不新增，返回已有单。

### 管理端

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/admin/auth/login` | GUEST | body：`username`,`password`。`data`：`token`,`adminId`,`username`,`displayName`,`role` |
| GET | `/api/admin/dashboard` | OPERATOR、AGENT | Demo：`pendingIntents`,`pendingMessages`。v1.1 加计数，见下 |
| GET | `/api/admin/intents` | OPERATOR、AGENT | 别名 `/api/admin/booking-intents`。`status?`,`page`,`pageSize` |
| GET | `/api/admin/intents/{id}` | OPERATOR、AGENT | 详情 + `notes` 时间线 |
| POST | `/api/admin/intents/{id}/status` | OPERATOR、AGENT | body：`status`,`note?`,`version?`。冲突 **409** |
| GET | `/api/admin/homestays` | OPERATOR、AGENT | 房源列表（客服只读使用此接口） |
| POST | `/api/admin/homestays/{id}/status` | **仅 OPERATOR** | 代为上下架。body：`status`=`ON_SALE`\|`OFF_SALE`\|`DRAFT`,`version?`。客服 **403** |
| GET | `/api/admin/contact-messages` | OPERATOR、AGENT | 留言列表 |
| POST | `/api/admin/contact-messages/{id}/done` | OPERATOR、AGENT | 标记已处理 |

意向状态：`PENDING` → `CONFIRMED` | `RESCHEDULED` | `CLOSED`。  
房源对客状态：`ON_SALE` 门户可订；`OFF_SALE` / `DRAFT` 详情 410，且不进门户列表。

意向列表字段（运营/客服，含完整手机号）：`id`,`intentNo`,`userId`,`homestayId`,`checkIn`,`checkOut`,`guests`,`contactName`,`contactPhone`,`status`,`version`,`createdAt`。备注：`id`,`adminId`,`action`,`note`,`createdAt`。

### CORS 与启动（Demo）

已放行源：`5173` / `5174` / `43123` / `43124`。

```bash
cd nongjiale-api
mvn spring-boot:run
# 基址 http://localhost:8080
```

---

## v1.1 增量（契约 · 未实现）

给 `apps/merchant` 以及门户/运营端补页用。路径、方法、鉴权、关键字段以下表为准。门店在商家 API 里叫 store，**等于**门户/运营的 homestay（`storeId` = `homestayId`）。

### 门户增量

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/me/intents` | 门户用户 | **只读**当前用户的意向。他人单据不出现 |
| GET | `/api/me/intents/{id}` | 门户用户 | 只读详情。不是本人 → **404** |
| GET | `/api/articles` | GUEST | 已发布攻略/资讯。`page`,`pageSize` |
| GET | `/api/articles/{id}` | GUEST | 详情。草稿或不存在 → **404** |
| POST | `/api/merchant-applications` | GUEST 或门户用户 | 商家入驻申请。提交后申请人 **不会**变成商家 |
| GET | `/api/me/merchant-application` | 门户用户 | 最近一条入驻申请（驳回原因、可否再提） |
| POST | `/api/merchant-applications/media` | GUEST 或门户用户 | 证照图片。`multipart` 字段 `file` |

不提供取消、改状态、支付接口。`POST /api/me/intents/{id}/cancel` 不存在（404）。

`GET /api/homestays` 增加可选查询（Demo 可暂忽略未知参数）：

- `tag`：特色标签，精确匹配
- `sort`：`default`（综合，默认同 Demo 按销量）| `price`（起价升序）| `sales`（销量降序）

`GET /api/me/intents` 的 `data.items[]`：

| 字段 | 说明 |
|------|------|
| `id`,`intentNo` | |
| `homestayId`,`homestayName` | |
| `checkIn`,`checkOut`,`guests` | |
| `status` | `PENDING`/`CONFIRMED`/`RESCHEDULED`/`CLOSED` |
| `createdAt` | |

详情在列表字段上增加 `contactName`,`contactPhone`（本人全号）、`travelMode`。无 `notes` 写入口；可不返回运营备注，避免把内部跟进暴露给游客。

入驻申请 body：

| 字段 | 必填 | 说明 |
|------|------|------|
| `subjectName` | 是 | 主体名称 |
| `contactName` | 是 | 联系人 |
| `phone` | 是 | 联系手机 |
| `proposedStoreName` | 是 | 拟营门店名 |
| `regionId` | 是 | 已有地区 id |
| `description` | 否 | 说明 |
| `licenseText` | 否 | 证照文本（一期可只填这个） |
| `licenseImageUrls` | 否 | 先走 media 接口拿到的 URL |

成功 `data`：`applicationId`,`status:"PENDING"`。已登录时记下 `userId`，供「我的申请」查询。  
已有 `PENDING` → **409**。已 `APPROVED` → **409**「已入驻」。`REJECTED` 后允许再提一条新申请。  
`GET /api/me/merchant-application`：按当前用户（及登录手机号与申请手机号一致的最近一条）返回；没有则 **404**。字段同审核详情里对申请人可见的子集：`applicationId`,`status`,`rejectReason`,`createdAt` 以及所填资料。

media 成功 `data`：`{ "url": "..." }`。仅图片；大小上限实现时定。URL 再写入申请或门店图集。

### 商家端 `/api/merchant/...`

全部要求商家 Bearer。`type=MERCHANT` 且 `role=MERCHANT`。门户 token 或运营 token 调用 → **403**。  
`mustChangePassword=true` 时，除登录、`GET /api/merchant/auth/me`、`POST /api/merchant/auth/password` 外，其余商家接口 **403**「请先修改初始密码」。

#### 登录与账号

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/merchant/auth/login` | body：`phone`,`password` |
| GET | `/api/merchant/auth/me` | 当前主体 |
| POST | `/api/merchant/auth/password` | body：`oldPassword`,`newPassword`。成功后 `mustChangePassword=false` |
| GET | `/api/merchant/dashboard` | `storeCount`,`pendingListingApplications`,`newIntentCount`（只读计数） |

登录成功 `data`：

```json
{
  "token": "<bearer>",
  "merchantUserId": 1,
  "merchantId": 10,
  "subjectName": "某某农庄",
  "phone": "13800001111",
  "role": "MERCHANT",
  "mustChangePassword": true
}
```

停用账号：401「账号已停用，请联系运营」。无短信验证码登录。

`me` 的 `data`：`merchantUserId`,`merchantId`,`subjectName`,`phone`,`role`,`mustChangePassword`。

#### 门店（仅本主体）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/merchant/stores` | 分页。只返回 token 对应 `merchantId` |
| POST | `/api/merchant/stores` | 新建，初始 `status=DRAFT`，游客不可订 |
| GET | `/api/merchant/stores/{storeId}` | 不是本主体 → **404** |
| PUT | `/api/merchant/stores/{storeId}` | 保存资料。`version` 冲突 **409** |
| DELETE | `/api/merchant/stores/{storeId}` | 仅 `DRAFT` 或 `OFF_SALE` 且没有待审上下架申请；否则 **422** |

可写字段：`name`,`regionId`,`address`,`phone`,`summary`,`coverUrl`,`gallery`(string[]),`facilities`,`notice`,`traffic`,`surroundings`,`version`。

请求里若带 `merchantId`,`status`,`salesCount`,`recommendWeight`：**不生效**（忽略）。上下架不走 PUT。

读模型在可写字段之外还有：`id`,`merchantId`,`status`(`DRAFT`|`ON_SALE`|`OFF_SALE`),`listingStatus`(`NONE`|`PUBLISH_PENDING`|`UNPUBLISH_PENDING`),`priceFrom`,`salesCount`,`version`。  
`priceFrom` 由套餐最低价汇总，商家不直接改。

保存资料 **不会**让下架门店变成可订。游客可订仅当 `status=ON_SALE`（运营审核通过或运营代为上架之后）。

#### 套餐与标签（仅本主体门店）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/merchant/stores/{storeId}/packages` | 套餐/价格档列表 |
| POST | `/api/merchant/stores/{storeId}/packages` | 新建 |
| PUT | `/api/merchant/stores/{storeId}/packages/{packageId}` | 更新 |
| DELETE | `/api/merchant/stores/{storeId}/packages/{packageId}` | 删除 |
| GET | `/api/merchant/stores/{storeId}/tags` | `{ "tags": ["亲子","烧烤"] }` |
| PUT | `/api/merchant/stores/{storeId}/tags` | 整体替换。body：`{ "tags": [] }` |

套餐字段：`name`,`description`,`priceFrom`,`sortOrder`。读时加 `id`,`storeId`。  
门店不属于当前商家 → **404**（套餐/标签同样）。  
实现时可把套餐落到现有 `homestay_room`，并补文案字段；对前端仍是 package 资源。

#### 上下架申请（方案 B）

商家不能调用 `POST /api/merchant/stores/{storeId}/status`（不提供，404）。不能把运营的 `/api/admin/homestays/{id}/status` 当商家接口用（商家 token → 403）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/merchant/stores/{storeId}/listing-applications` | body：`action`=`PUBLISH`\|`UNPUBLISH`，`note?` |
| GET | `/api/merchant/listing-applications` | 本主体申请。`storeId?`,`status?`,`page`,`pageSize` |
| GET | `/api/merchant/listing-applications/{id}` | 不是本主体 → **404** |

申请 `status`：`PENDING` → `APPROVED` | `REJECTED`。  
同一门店已有同 `action` 的 `PENDING` → **409**。  
`PUBLISH` 时门店已是 `ON_SALE` → **422**；`UNPUBLISH` 时不是 `ON_SALE` → **422**。  
提交后游客侧状态不变，直到运营通过。驳回后 `rejectReason` 对本商家可见，可再提。

申请读模型：`id`,`storeId`,`storeName`,`merchantId`,`action`,`status`,`note`,`rejectReason`,`createdAt`,`reviewedAt`。

#### 意向（M1 只读）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/merchant/intents` | 本主体门店下的意向。`storeId?`,`status?`,`page`,`pageSize` |
| GET | `/api/merchant/intents/{id}` | 不是本主体门店 → **404** |
| POST | `/api/merchant/intents/{id}/status` | **403**「商家不可修改意向或写跟进」 |
| POST | `/api/merchant/intents/{id}/notes` | **403**，同上 |

列表/详情字段：`id`,`intentNo`,`storeId`,`storeName`,`checkIn`,`checkOut`,`guests`,`status`,`contactName`,`contactPhoneMasked`,`noteSummary`,`createdAt`。

- `contactPhoneMasked`：保留前 3 后 4，例如 `138****5678`。响应中不得出现完整 `contactPhone`
- `noteSummary`：最新一条跟进的摘要文本，可为空串。不返回 `adminId`，不提供备注写入
- 上述两个 POST 即使 body 合法也拒绝，库中状态与备注不变。PATCH/PUT 同等路径同样 403
- 这两个写路径对商家 **一律 403**（单据不存在、属于本主体、属于其他商家都一样），避免用 404/403 探测他人意向是否存在

### 运营端增量 `/api/admin/...`

既有意向跟进（列表、详情、改状态+备注）**保持运营与客服可用**，见 Demo 表。商家端没有对等写接口。

客服（`AGENT`）调用本节写接口，以及入驻队列、上下架队列、站点配置、商家账号 → **403**，数据不变。客服仍可改意向、处理留言、只读房源列表。

#### 入驻审核（仅 OPERATOR）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/merchant-applications` | `status?`,`page`,`pageSize`。待审优先由前端按 `status=PENDING` 筛 |
| GET | `/api/admin/merchant-applications/{id}` | 资料全文 + `licenseText` + `licenseImageUrls` |
| POST | `/api/admin/merchant-applications/{id}/approve` | 通过并开通商家账号 |
| POST | `/api/admin/merchant-applications/{id}/reject` | body：`reason` 必填，空则 400 |

通过成功 `data`：

```json
{
  "applicationId": 1,
  "status": "APPROVED",
  "merchantId": 10,
  "loginPhone": "13800001111",
  "initialPassword": "<仅此响应返回一次>",
  "mustChangePassword": true
}
```

`loginPhone` 默认用申请单手机。不发短信。`initialPassword` 不得出现在后续列表接口。  
非 `PENDING` 再审 → **409**。驳回写入 `rejectReason`，申请人可再提。

#### 商家账号（仅 OPERATOR，入驻通过之后）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/merchants` | `page`,`pageSize`。字段：`merchantId`,`subjectName`,`loginPhone`,`enabled`,`storeCount` |
| POST | `/api/admin/merchants/{merchantId}/enabled` | body：`{ "enabled": false }` |
| POST | `/api/admin/merchants/{merchantId}/reset-password` | `data`：`initialPassword`,`mustChangePassword:true`（同样只返回一次） |
| POST | `/api/admin/homestays/{id}/owner` | body：`{ "merchantId": 10 }`。调整门店归属。客服 **403** |

#### 上下架审核（仅 OPERATOR，方案 B）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/listing-applications` | `status?`,`action?`,`page`,`pageSize` |
| GET | `/api/admin/listing-applications/{id}` | 含门店摘要与商家主体名 |
| POST | `/api/admin/listing-applications/{id}/approve` | 通过后 **对游客生效** |
| POST | `/api/admin/listing-applications/{id}/reject` | body：`reason` 必填 |

通过时：

- `action=PUBLISH` → 门店 `status=ON_SALE`，门户列表/详情可订
- `action=UNPUBLISH` → 门店 `status=OFF_SALE`，门户详情 **410**，退出列表

驳回：门店 `status` 不变，商家可见 `rejectReason`。非 `PENDING` → **409**。

运营代为上下架仍用 Demo 的 `POST /api/admin/homestays/{id}/status`（仅 OPERATOR）。代操作不要求先有商家申请。

#### 站点配置（仅 OPERATOR）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/admin/site-config` | OPERATOR | 当前配置。客服 403 |
| PUT | `/api/admin/site-config` | OPERATOR | 保存 |
| GET | `/api/config/public` | GUEST | 门户读取。Demo 已有，v1.1 **加字段** |

body / 公开 `data`：

| 字段 | 说明 |
|------|------|
| `siteName` | 站名。老板未定前可为占位或空串 |
| `primaryColor` | 主色，`#RRGGBB`。未定前可空 |
| `contactPhone` | 客服电话。Demo 默认 `13651814168`，保存后门户成功页与 `tel:` 用新值 |

正式值未到位时允许占位；三个字段都要出现在 v1.1 的 public 响应里（值可为空串）。Demo 进程在实现前只返回 `contactPhone`，前端对缺字段做兼容。

#### 资讯（站点内容，写仅 OPERATOR）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/admin/articles` | OPERATOR | `status?`,`page`,`pageSize`。客服 403 |
| POST | `/api/admin/articles` | OPERATOR | 新建，默认 `DRAFT` |
| PUT | `/api/admin/articles/{id}` | OPERATOR | 更新 |
| POST | `/api/admin/articles/{id}/status` | OPERATOR | body：`status`=`DRAFT`\|`PUBLISHED` |

字段：`title`,`coverUrl`,`summary`,`body`。读模型加 `id`,`status`,`publishedAt`,`createdAt`。  
门户只读 `PUBLISHED`。不做问答、私人定制、专题频道接口。

#### 工作台计数（在 Demo 字段上追加）

`GET /api/admin/dashboard` 的 `data` 在 `pendingIntents`,`pendingMessages` 之外增加：

- `pendingMerchantApplications`（客服固定为 0 或字段省略；不要为此给客服开审核数据）
- `pendingListingApplications`（同上）

客服响应保持 Demo 两个字段即可，避免侧栏没有入口却看到审核计数。运营响应四个字段都给。

---

## 数据隔离

商家维度是 `merchantId`（主体），不是「当前点开的那一家店」单独隔离：一个主体下多门店，这些门店的资料、套餐、标签、上下架申请、意向，商家都能看；**其他主体的一律不能**。

1. 列表查询在服务端加 `merchant_id = token.merchantId`。结果为空数组，而不是 403。
2. 按 id 读取或修改他人门店、套餐、申请、意向 → **404**（「门店不存在」/「意向单不存在」），响应体不带回对方字段。
3. body/query 显式传了别的 `merchantId` → **403**。
4. 意向归属通过门店的 `merchantId` 判断，不信任客户端传来的 `storeId` 与意向的组合：意向的门店必须属于当前商家。
5. 商家意向接口不返回完整手机号、不返回 `adminId`。
6. 运营可跨商家查看和代改，并可 `POST /api/admin/homestays/{id}/owner` 改归属。改归属后，原商家立即失去该店及其意向。
7. 客服可以看全站意向（跟进需要），不能看入驻材料队列、不能改归属、不能审上下架。
8. 门户「我的意向」只按 `userId = token.id` 过滤，与商家隔离是两条线。
9. 前端隐藏按钮不算隔离；上述规则在接口层执行。

---

## 明确不做（v1.1 无路径）

支付 / 定金收银台 · 结算 / 对账 / 提现 / 分账 · 锁房 / 库存日历 · 积分 · 短信 OTP / 短信查单 · IM · 问答 / 私人定制 / 专题频道 · 地图找店 · App · 门票 · 商家营销券 / 电子签。

不提供例如 `/api/payments`、`/api/merchant/settlements`、`/api/inventory`、`/api/sms`、`/api/im`。这些路径保持 404，不要做半成品。

---

## 实现备注（非本 PR 范围）

- 商家端本地源建议放行 `http://localhost:5175`（改 `nongjiale.cors-allowed-origins`）。本 PR 不改配置。
- 登录响应与 claim 扩展、`/api/me/intents`、`/api/merchant/**`、入驻/上下架审核、站点配置读写，都等后续实现 PR。
- 文档中心就绪后迁入统一目录；变更同步项目经理。
