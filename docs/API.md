# 农家乐 API / 数据说明（B1）

对齐 P1：方案 A · 必须登录提交 · 无我的订单 · 无支付锁房 · `contactPhone=13651814168`。

## 栈

- Java 17（本地可用 JDK 21 编译）· Spring Boot 3.3 · Web · Validation · JPA · Flyway · Security
- 本地默认 H2 内存库；生产可切 MySQL（改 `application.yml`）

## 迁移

| 脚本 | 内容 |
|------|------|
| `V1__init_schema.sql` | 用户、管理端、地区、房源、房型、评价、资讯、推荐位、意向单、备注、留言 |
| `V2__seed_yangtze_delta.sql` | 长三角地区 + 演示账号 + 样例房源 |

演示账号（仅开发/测试）：游客 `13800000001` / `password`；运营 `operator` / 客服 `agent` / `password`。

## 统一约定

- 响应：`{ "code": 0, "message": "ok", "data": ... }`；业务错误非 0，HTTP 与语义对齐（401/403/400/404/409/410）
- 列表：`data: { items, total }`；分页 `page` 从 1 起
- 鉴权：`Authorization: Bearer <token>`（一期内存 Session，契约稳定可换 JWT）

## 门户

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 无 | 注册并发 token |
| POST | `/api/auth/login` | 无 | 手机号登录 |
| GET | `/api/auth/me` | 游客 | 当前用户 |
| GET | `/api/config/public` | 无 | `contactPhone` |
| GET | `/api/regions` | 无 | 启用地区；`data` 为**纯数组**（非 `{items,total}`） |
| GET | `/api/home/feed` | 无 | 首页摘要 |
| GET | `/api/homestays` | 无 | 过滤分页（仅上架）；查询参数：`regionId`/`regionSlug`/`keyword`/`priceMin`/`priceMax`/`page`/`pageSize`。门户 URL 用 `q`，由前端映射为 `keyword` |
| GET | `/api/homestays/{id}` | 无 | 详情；下架 **410** |
| POST | `/api/booking-intents` | 游客 | 提交意向；幂等键 `userId:homestayId:checkIn` |
| POST | `/api/contact-messages` | 游客 | 咨询留言 |

不做：`GET /api/me/intents`、支付、库存。

## 管理端

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/admin/auth/login` | 无 | 运营/客服登录 |
| GET | `/api/admin/dashboard` | 管理 | 待跟进意向/留言数 |
| GET | `/api/admin/intents` | 管理 | 筛状态分页 |
| GET | `/api/admin/intents/{id}` | 管理 | 详情+备注时间线 |
| POST | `/api/admin/intents/{id}/status` | 管理 | 改状态+备注；`version` 冲突 **409** |
| GET | `/api/admin/homestays` | 管理 | 房源列表 |
| POST | `/api/admin/homestays/{id}/status` | **仅 OPERATOR** | 上下架；客服 **403** |
| GET | `/api/admin/contact-messages` | 管理 | 留言列表 |
| POST | `/api/admin/contact-messages/{id}/done` | 管理 | 标记已处理 |

意向状态：`PENDING` → `CONFIRMED` | `RESCHEDULED` | `CLOSED`。

## CORS 已放行：`5173`/`5174`/`43123`/`43124`。

## 本地启动

```bash
cd nongjiale-api
mvn spring-boot:run
# 基址 http://localhost:8080
```

文档中心就绪后迁入统一目录；变更同步项目经理。
