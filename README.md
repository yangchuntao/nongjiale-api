# 苏中种子 · 农家乐 API

`nongjiale-api` 是苏中农家乐垂直平台的后端。当前是 MVP-0 的 S1 脚手架：能启动、Flyway 建表并写入种子、运营用手机号登录。门户、商家和支付接口还没做。

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
com.suzhong.njl.web             信封、健康检查、运营登录
com.suzhong.njl.service         运营登录
com.suzhong.njl.domain.entity   表对应的实体
com.suzhong.njl.domain.enums    状态、角色、地区层级
com.suzhong.njl.mapper          MyBatis-Plus Mapper
```

本次不做的业务接口：门户目录、意向提交、入驻审核、商家端、支付。

## 门店文案与验收数据

门店详情和验收以 `store.package_text` 为准。`store_package` 可以没有行；两处不一致时以 `package_text` 为准。

当前 V2 种子只有运营账号、苏中地区和站点配置，没有房源。验收用的精选店是手工整理的（含表亲店），不是抓取结果。抓取或导入的房源必须把 `test_data` 设为 true。
