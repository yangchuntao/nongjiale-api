# 苏中种子 · 农家乐 API

`nongjiale-api` 是苏中农家乐垂直平台的后端。MVP-0 已包含门户、商家和运营审核。没有支付、锁房和佣金计算。

## 技术栈

- Java 17（本机可用 21 编译，发布目标是 17）
- Spring Boot **3.3.13**（从骨架上的 3.5.16 降下来，对齐 TECH）
- MyBatis-Plus 3.5.9，没有 Spring Data JPA
- Flyway
- Spring Security，Bearer，密码 BCrypt
- Redis 只做缓存。登录令牌在进程内存里，不进 Redis
- 本地 H2（`MODE=MySQL`），MySQL 8 用 `mysql` profile

包名是 `com.suzhong.njl`。骨架里的 `com.suzhong.seed` 已替换，和仓库名对齐，避免再留一个 seed 空壳。

## 本地运行

需要 JDK 17+、Maven 3.8+。默认 profile 是 `local`。

```bash
mvn test
mvn spring-boot:run
```

`mvn test` 会跑 JaCoCo，并用 **行覆盖率（LINE / COVEREDRATIO）** 做门禁。整包低于 85% 时构建失败。报告在 `target/site/jacoco/index.html`（XML 为 `target/site/jacoco/jacoco.xml`）。说明见 `docs/COVERAGE.md`。

服务端口 `8080`。健康检查 `GET /health` 不走业务信封，返回：

```json
{"status":"UP","service":"nongjiale-api"}
```

切 MySQL：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

环境变量：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`，以及 `REDIS_HOST`、`REDIS_PORT`。

登录和建表不连接 Redis。真正读写缓存（首页、门店详情、地区、站点配置）之前，需要本机 Redis。令牌仍然只放内存。

## 运营登录

`POST /api/admin/auth/login`

```json
{"phone":"13800000000","password":"SeedAdmin#2026"}
```

成功信封：

```json
{"code":0,"message":"ok","data":{"token":"...","role":"OPERATOR"}}
```

之后访问 `/api/admin/**` 带 `Authorization: Bearer <token>`。没有 `username` 字段，多传会被拒绝。

这组手机号和密码只在 Flyway 种子里，给本地和测试用。正式环境必须换掉。

`GET /api/admin/auth/me` 用来确认当前运营身份。

## CORS

放行：

- `http://localhost:43123` 门户
- `http://localhost:43124` 运营
- `http://localhost:43125` 商家

配置键：`app.cors-allowed-origins`。Bearer 不带 cookie，`allowCredentials` 为 false。

## 配置

| 键 | 作用 |
|----|------|
| `server.port` | 默认 8080 |
| `spring.datasource.*` | `local` 为 H2，`mysql` 为 MySQL |
| `mybatis-plus.*` | 驼峰、枚举 code；不自动建表 |
| `spring.flyway.*` | V1 结构，V2 苏中种子 |
| `spring.data.redis.*` | 缓存连接 |
| `app.cors-allowed-origins` | 三个本地前端源 |
| `app.security.token-ttl` | 内存令牌有效期，默认 12 小时 |
| `app.cache.key-prefix` | Redis 键前缀，默认 `njl:` |
| `app.cache.*-ttl` | 首页、门店详情、地区、站点配置 |

## 包

```
com.suzhong.njl                 启动
com.suzhong.njl.config          Security、CORS、Redis、MyBatis-Plus、时钟
com.suzhong.njl.security        内存令牌与 Bearer
com.suzhong.njl.web             信封、健康检查、三端接口
com.suzhong.njl.service         门户、商家、运营业务
com.suzhong.njl.domain.entity   表对应的实体
com.suzhong.njl.domain.enums    状态、角色、地区层级
com.suzhong.njl.mapper          MyBatis-Plus Mapper
```

门户见 `BE4-DELIVERY.md`，商家端见 `BE5-DELIVERY.md`，运营业务见 `BE6-DELIVERY.md` 和 `docs/ADMIN-API.md`。开城见 `BE7-DELIVERY.md`，契约见 `docs/TECH-DESIGN-MVP-1-开城.md`。没有支付。没有控制器的路径（含 `/api/payments`）返回 HTTP 404，信封 `{code:404, message:接口不存在, data:null}`，不是 500。见 `docs/S5-E01-UNKNOWN-ROUTE.md`。

MVP-1 开城技术设计（设计稿，未写业务代码）：`docs/TECH-DESIGN-MVP-1-开城.md`。
MVP-1 开城技术设计产品门禁（PASS）：`docs/GATE-TECH-DESIGN-MVP-1-开城-产品.md`。

## 门户

- `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`：游客手机号，令牌角色 `TOURIST`
- `GET /api/config/public`、`GET /api/regions`、`GET /api/home/feed`、`GET /api/home/banners`
- `GET /api/home/banners`：公开，无城市参数。只返回启用条目：全站始终在内，指定市仅该市当前仍开通。按排序升序，数字越小越前。0 条是空数组。不进首页 feed 缓存。图片只收 URL
- `GET /api/regions`：只返回已开通的市，以及这些市下面仍启用的区县。没有开通市的省不出现
- `GET /api/stores`：只列出已开通市里的上架房源。未开通市返回空列表，`destinationStatus=CLOSED`，`emptyHint` 为「该目的地暂未开通」。可按地区、标签、价格带筛选，排序 `comprehensive` / `price_asc` / `price_desc`
- `GET /api/stores/{id}`：下架或所属市未开通返回 410「已下架或不可用」。套餐以 `packageText` 为准
- `GET /api/home/feed`：首页推荐只保留已开通市的上架房源。未开通市的攻略不占首页名额
- `GET /api/articles`：不列出未开通市的攻略。已发布攻略的直链仍可打开
- `POST /api/booking-intents`、`GET /api/me/intents`：须游客登录，只能看自己的意向
- `GET /api/articles`：只返回已发布攻略
- `POST /api/merchant-applies`：提交入驻申请

V3 种子是手工精选店，含「表弟店」，`test_data=false`。抓取或导入的房源要标 `test_data=true`，列表和详情会带「测试」。

## 商家

`POST /api/merchant/auth/login` 使用商家手机号和密码。主体或账号停用返回 403。令牌角色是 `MERCHANT`，只能访问 `/api/merchant/**`。

- `GET /api/merchant/stores`、`GET/PUT /api/merchant/stores/{id}`：只看本主体。不能直接改上下架和首页推荐，也没有创建门店接口
- `POST /api/merchant/stores/{id}/shelf-applications`：申请上架或下架。同一门店同时只能有一条待审
- `GET /api/merchant/shelf-applications`：本主体申请
- `GET /api/merchant/intents`、`GET /api/merchant/intents/{id}`：只读，联系电话中间四位脱敏

商家账号由运营审核入驻后开通，见下方运营路径。本端没有创建门店接口。

## 运营

除登录外，`/api/admin/**` 需要 `OPERATOR` 或 `AGENT` 的 Bearer。未登录 401。游客或商家令牌 403。字段表见 `docs/ADMIN-API.md`。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/auth/login` | 手机号 + 密码 |
| GET | `/api/admin/auth/me` | 当前运营 |
| GET | `/api/admin/merchant-applies` | 入驻队列，可按 `status` 筛 |
| POST | `/api/admin/merchant-applies/{id}/approve` | 通过并开通账号。明文密码只在当次响应 |
| POST | `/api/admin/merchant-applies/{id}/reject` | 驳回，`reason` 必填。非待审 409 |
| GET | `/api/admin/merchants` | 商家主体列表 |
| PATCH | `/api/admin/merchants/{orgId}` | 主体 `status` 或账号 `enabled`。停用后旧令牌失效 |
| POST | `/api/admin/merchants/{orgId}/reset-password` | 重置该主体登录密码，明文只返回一次 |
| POST | `/api/admin/merchants/{orgId}/stores` | 代建门店并绑定主体 |
| GET | `/api/admin/stores`、`GET /api/admin/stores/{id}` | 全站门店 |
| PUT | `/api/admin/stores/{id}` | 代编。可写 `homeRecommended`、`homeSort`、`shelfStatus`。乐观锁 `version` |
| GET | `/api/admin/shelf-applications` | 上下架申请队列 |
| POST | `/api/admin/shelf-applications/{id}/approve` | 通过后改门店上架状态 |
| POST | `/api/admin/shelf-applications/{id}/reject` | 驳回，原因必填。游客可见性不变 |
| GET | `/api/admin/intents`、`GET /api/admin/intents/{id}` | 意向列表和详情。运营看完整手机号 |
| POST | `/api/admin/intents/{id}/status` | `targetStatus` + `version`，可选 `note` |
| POST | `/api/admin/intents/{id}/notes` | 只追加备注 |
| GET | `/api/admin/intents/export` | CSV，筛选与列表相同。无佣金列 |
| GET/POST/PUT/DELETE | `/api/admin/articles` | 攻略。发布后门户可见 |
| GET/PUT | `/api/admin/site-config` | 站名、主色、客服电话。不改开城阈值 |
| GET/POST | `/api/admin/banners` | 首页 Banner 列表与新建。图片只填 URL |
| GET/PUT/DELETE | `/api/admin/banners/{id}` | 详情、全量更新、物理删除 |
| POST | `/api/admin/banners/{id}/enable`、`/disable` | 行内启停。启用时投放市已关闭返回 409 |
| GET | `/api/admin/cities`、`GET /api/admin/cities/{cityId}` | 开城列表和详情。供给、攻略按实时计数 |
| PATCH | `/api/admin/cities/{cityId}/checklist` | 只手改三项。带供给/攻略达标字段返回 400 |
| POST | `/api/admin/cities/{cityId}/open`、`POST /api/admin/cities/{cityId}/close` | 达标才开通。未达标 409。关城不删历史 |
| GET | `/api/admin/metrics/city-intents` | 意向 / 已确认 / 已入住汇总。`window` 为 `7`、`30`、`ALL` |
| GET | `/api/admin/metrics/city-intents/export` | 同上筛选的 CSV |

代上下架写在 `PUT /api/admin/stores/{id}` 的 `shelfStatus`，没有单独的 shelf 接口。与当前状态不同则立即生效，并写 `SHELF_FORCE` 审计。不走申请队列。

`CHECKED_IN` 的中文是「已入住」。站点主色种子保持 `#4BA3D9`。

## 门店文案与验收数据

门店详情和验收以 `store.package_text` 为准。`store_package` 可以没有行；两处不一致时以 `package_text` 为准。

V2 仍只有运营账号、苏中地区和站点配置。验收精选店在 V3，含表弟店，`test_data=false`。抓取或导入的房源必须把 `test_data` 设为 true。
