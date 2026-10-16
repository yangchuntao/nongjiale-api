# BE-5 商家 API 交付

仓库：`andrew-yang/nongjiale-api`。在已合并的门户 API 和品牌色种子上新增商家端。不包含运营审入驻、代建门店和支付。

## ① 概要设计

商家登录后只能维护**本主体**已有门店、提交上下架申请，并只读本主体意向。令牌角色是 `MERCHANT`，带 `merchantUserId` 和 `merchantOrgId`，不能调用门户或运营接口。

没有创建门店的接口。上下架不直接改 `shelf_status`，只写入 `PENDING` 申请。同一门店同时最多一条待审。意向没有写接口，联系电话按 `(\d{3})\d{4}(\d{4})` 脱敏。

响应仍是 `{code, message, data}`。状态和动作使用已有枚举：`MerchantOrgStatus`、`ShelfStatus`、`ShelfAction`、`ReviewStatus`、`UserType`、`IntentStatus`。

## ② 详细设计

### 接口

| 方法 | 路径 | 鉴权 | 结果 |
|------|------|------|------|
| POST | `/api/merchant/auth/login` | 无 | 手机号 + 密码。成功返回 Bearer，`role=MERCHANT` |
| GET | `/api/merchant/stores` | 商家 | 仅 `merchant_org_id = principal.merchantOrgId` |
| GET | `/api/merchant/stores/{id}` | 商家 | 本店。别的主体按不存在返回 404 |
| PUT | `/api/merchant/stores/{id}` | 商家 | 改文案和地址。请求里出现 `homeRecommended`、`homeSort`、`shelfStatus` 则 400，且不落库 |
| POST | `/api/merchant/stores/{id}/shelf-applications` | 商家 | `action=PUBLISH\|UNPUBLISH`，状态 `PENDING`。已有待审则 409。不改当前上架状态 |
| GET | `/api/merchant/shelf-applications` | 商家 | 本主体申请 |
| GET | `/api/merchant/intents`、`/{id}` | 商家 | 只读本主体。手机号 `138****8000` 这种形式。库里仍是完整号 |

未登录访问商家接口是 401。游客或运营令牌是 403。主体 `DISABLED` 或账号 `enabled=false`：登录 403，已发出的令牌再次访问也是 403。密码错误是 401，文案「手机号或密码错误」。

跨主体的门店、申请和意向都是 404，不说明记录是否存在。列表 SQL 带 `merchant_org_id`，不在查出全表后再过滤。

商家更新使用门店 `version`。版本不一致返回 409。写成功后失效门店详情和首页缓存；Redis 删除失败只记日志。

门户下架 410 不变。商家申请下架在运营通过前，游客仍按当前 `shelf_status` 看房。

入驻提交仍是门户 `POST /api/merchant-applies`。本任务不审申请、不代建门店。测试直接插入启用的 `merchant_org`、`merchant_user` 和绑定门店。

### 隔离与脱敏

- 登录主体：`AuthenticatedUser.userId` 是商家用户 id，`merchantOrgId` 是隔离键，`authority()` 为 `MERCHANT`。
- 每次商家业务请求再核对账号启用和主体 `ACTIVE`。
- 意向输出走 `PhoneMask.maskContact`。日志不打印完整手机号。

## ③ 改动点

| 对象 | 动作 |
|------|------|
| `SecurityConfig` | 商家登录公开；`/api/merchant/**` 仅 `MERCHANT` |
| `web.merchant` / `service.merchant` | 登录、本店读写、上下架申请、意向只读 |
| `MerchantAccess` | 停用校验，并取出当前主体 id |
| `PhoneMask.maskContact` | 商家意向手机号中间四位 |
| `CacheAside.evict` | 商家改门店后失效详情和首页缓存 |
| 测试 | 登录与停用、令牌不串用、两商家隔离、拒绝推荐和上下架直改、待审唯一、脱敏与无写意向 |
| `README.md` | 补商家路径 |

## ④ 影响点

- 商家前端可以用 43125 已放行的 CORS 对接上表。运营审核上下架和入驻仍未提供，申请会停在 `PENDING`，游客可见性不变。
- 门店更新请求不要带推荐字段和 `shelfStatus`，否则整单 400。
- 已有库不需要新的 Flyway 版本。商家账号要先有主体和用户行才能登录。
- 游客、运营、商家三类令牌仍然不能互调。
- 门户详情继续以 `packageText` 为套餐权威文案。商家改的是这一列，不改门户 410 规则。
