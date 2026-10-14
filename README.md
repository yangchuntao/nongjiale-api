# nongjiale-api

农家乐旅游网站后端（方案 A：意向单 + 客服跟进）。Spring Boot 3.3 / Java 17 / JPA / Flyway / Security。

## 本地启动

```bash
mvn spring-boot:run
```

默认 H2 内存库 + Flyway 种子（长三角地区、演示游客 `13800000001` / `password`，运营 `operator` / `password`，客服 `agent` / `password`）。商家甲 `13800001111`、商家乙 `13800002222`，密码同为 `password`。v1.1 路径见 `docs/API.md`。

## 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 游客登录 |
| POST | `/api/auth/register` | 游客注册 |
| GET | `/api/config/public` | 含 `contactPhone=13651814168` |
| GET | `/api/regions` | 地区 |
| GET | `/api/homestays` | 列表分页 |
| GET | `/api/homestays/{id}` | 详情（下架 410） |
| POST | `/api/booking-intents` | 提交意向（须 Bearer） |
| POST | `/api/contact-messages` | 咨询留言（须 Bearer） |
| POST | `/api/admin/auth/login` | 管理端登录 |
| GET | `/api/admin/intents` | 意向列表 |
| POST | `/api/admin/intents/{id}/status` | 跟进改状态（version→409） |
| POST | `/api/admin/homestays/{id}/status` | 上下架（仅 OPERATOR） |

统一响应：`{ code, message, data }`。无「我的订单」、无支付。

## 文档

- [接口与表结构 / 迁移说明](docs/API.md)
- 测试文档见 [docs/TEST.md](docs/TEST.md)（由测试维护）

## 文档

- [产品范围与验收](docs/PRODUCT.md)
- [API / 数据](docs/API.md)
- [测试](docs/TEST.md)
