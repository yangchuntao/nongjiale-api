# S1 后端脚手架交付

仓库：`andrew-yang/nongjiale-api`。依据 TECH-DESIGN-MVP-0，在已有 Spring Boot 骨架上演进，没有另起仓库。

## ① 概要

把空仓收成可启动的一期底座：

- Java 17，Spring Boot **3.3.13**（由 3.5.16 下调，没有必须留在 3.5 的阻塞）
- MyBatis-Plus，Flyway 管表，不用 JPA
- 本地 H2 `MODE=MySQL`，`mysql` profile 指向 MySQL 8
- Redis 只挂缓存；运营登录是手机号 + 密码，Bearer 令牌放进程内存
- CORS 只放行 `43123` / `43124` / `43125`
- 统一信封 `{code, message, data}`
- 状态和角色走枚举，服务层不写死码值字符串

包名定为 `com.suzhong.njl`，构件 `nongjiale-api`。骨架包 `com.suzhong.seed` 只覆盖启动类和健康检查，改掉比长期留两个名字更清楚。

本期没有门户、商家、意向、支付接口。

## ② 详细

### 鉴权

- `POST /api/admin/auth/login`，请求体只有 `phone`、`password`
- 成功 `code = 0`，`data` 含 `token`、`expiresAt`、`adminId`、`role`、`displayName`
- 密码错误：HTTP 401，`code = 401`，文案「手机号或密码错误」
- 密码正确但停用：HTTP 403，「账号已停用」
- `/api/admin/**` 需要 `OPERATOR` 或 `AGENT`。未登录 401「未登录」，角色不对 403「无权限」
- `GET /api/admin/auth/me` 只用来核对当前运营身份
- CSRF 关闭，会话无状态。令牌是 32 字节随机串，存在 `InMemoryTokenStore`，过期即丢
- 种子账号：手机号 `13800000000`，密码 `SeedAdmin#2026`，角色 `OPERATOR`，显示名「苏中运营」

`GET /health` 仍是探针，不包信封。

### 数据

Flyway `V1__schema.sql` 建 TECH §2.2 的核心表。枚举列 `VARCHAR(32)`。`admin_user` 只有 `phone` 唯一键，没有 `username`。`store.version`、`booking_intent.version` 给乐观锁。意向表没有支付列。

大文本用 `TEXT`，这样 H2 MySQL 模式和 MySQL 8 能共用脚本。同一门店「最多一条待审上下架」不建部分唯一索引，两边语法不一致，留给以后的服务层。

门店详情和验收以 `store.package_text` 为权威文案。`store_package` 允许没有行；若明细和主文案不一致，以 `package_text` 为准。

`V2__seed_suzhong.sql` 写入：

- 上述运营账号（BCrypt）
- 苏中地区树：江苏，以及扬州、泰州、南通和广陵、海陵、崇川
- `site_config`：站名「站点名称」、主色 `#4BA3D9`、电话 `400-000-0000`

V2 不写门店。验收房源是手工精选（含表亲店），不使用抓取列表。抓取或导入的房源必须 `test_data=true`，不能当成验收店。

`CHECKED_IN` 的中文是「已入住」。意向状态机写在 `IntentStatus.canTransitTo`，非法迁移规则已有单测，但还没有对外改状态接口。

`store_tag_rel` 是联合主键，Mapper 不提供单主键 `selectById`。

### 缓存与时钟

`RedisCacheManager` 键前缀 `app.cache.key-prefix`（默认 `njl:`）。首页、门店详情、地区、站点配置各自有 TTL。缓存名是 `CacheName` 枚举。

应用时钟和 Jackson 时区都是 `Asia/Shanghai`。MySQL URL 带 `serverTimezone=Asia/Shanghai`。

### 依赖取舍

- MyBatis-Plus 3.5.9 与 Boot 3.3 对齐。分页插件在 `mybatis-plus-jsqlparser`，乐观锁仍在主包。
- H2 钉在 2.2.220，这是 Flyway 10.10 声明测过的版本。
- `spring-boot-autoconfigure` 里仍有 `HibernateJpaAutoConfiguration` 这个类文件，但没有 `jakarta.persistence`、Hibernate、`spring-data-jpa`。测试锁的是这三项不在类路径上。

## ③ 改动点

| 对象 | 动作 |
|------|------|
| `pom.xml` | Boot 3.3.13；Web、Validation、Security、Redis、Cache、MyBatis-Plus、Flyway、H2、MySQL；不引入 JPA |
| `com.suzhong.seed` | 删除，改为 `com.suzhong.njl` |
| `application*.yml` | `local` / `mysql`、CORS、令牌 TTL、缓存前缀 |
| `db/migration/V1__schema.sql` | 核心表、索引、外键、乐观锁；注明 `package_text` 优先、`test_data` 口径 |
| `db/migration/V2__seed_suzhong.sql` | 运营手机号、苏中地区、站点配置；不写验收房源 |
| `domain.enums` / `domain.entity` / `mapper` | 枚举、实体、Mapper |
| `security` + `AdminAuthController` | 内存令牌、Bearer、运营登录 |
| `RedisCacheConfig` | 缓存键前缀与 TTL，不存放令牌 |
| 测试 | 上下文、登录与鉴权信封、CORS、种子、类路径无 JPA、意向状态机 |
| `README.md`、`DELIVERY.md` | 本说明 |

## ④ 影响点

- 前端三个本地端口必须用 43123 / 43124 / 43125。运营登录表单字段是手机号，不是用户名。
- 响应请先看 HTTP，再读 `code` 和 `message`。健康检查不是这套信封。
- 本地默认仍是 H2。预发和生产要切 `mysql`，并准备 Redis。缓存未调用时，进程可以在没有 Redis 的情况下完成登录。
- 令牌重启即失效。不要把 Redis 配成会话库。
- 表结构只加 Flyway 版本，不要开 MyBatis-Plus 自动建表。
- 种子密码会出现在库脚本和文档里，只能用于本地。上线前更换。
- 运营审核、代建门店、导出和支付还没实现。门户见 `BE4-DELIVERY.md`，商家端见 `BE5-DELIVERY.md`。状态机枚举已经就位，后续接口应复用，不要再引入另一套字符串状态。
- 后续门店详情以 `package_text` 展示和验收。`store_package` 没有行仍算完整门店；明细与主文案不一致时，展示和验收都用 `package_text`。
- 验收房源是手工精选店（含表亲店）。抓取或导入的房源标 `test_data=true`。
