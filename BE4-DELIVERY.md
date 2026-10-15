# BE-4 门户 API 交付

仓库：`andrew-yang/nongjiale-api`。在已合并的 S1 上新增门户接口，不包含运营审核、商家端和支付。

## ① 概要

按 TECH §2.3.1 打开游客侧接口：注册登录、公开配置、地区、首页、上架房源、意向、已发布攻略，以及入驻申请提交。

响应仍是 `{code, message, data}`。游客令牌角色是 `TOURIST`，和运营令牌不能互调。热点读走 Redis cache-aside，Redis 失败时改读数据库。

验收房源在 Flyway V3，是手工精选，含「表弟店」，`test_data=false`。

## ② 详细

### 接口

| 方法 | 路径 | 鉴权 | 结果 |
|------|------|------|------|
| POST | `/api/auth/register` | 无 | 手机号 + 密码，返回 Bearer |
| POST | `/api/auth/login` | 无 | 同上，角色 `TOURIST` |
| GET | `/api/auth/me` | 游客 | 本人资料 |
| GET | `/api/config/public` | 无 | 站名、主色、客服电话 |
| GET | `/api/regions` | 无 | 启用地区 |
| GET | `/api/home/feed` | 无 | 上架且首页推荐的门店 + 已发布攻略摘要 |
| GET | `/api/stores` | 无 | 仅 `ON_SHELF`。查询 `regionId`、`tag`、`priceMin`、`priceMax`、`sort`、`page`、`pageSize` |
| GET | `/api/stores/{id}` | 无 | 详情。不存在 404，非上架 410「已下架或不可用」 |
| POST | `/api/booking-intents` | 游客 | 创建 `PENDING` 意向，返回 `intentNo` |
| GET | `/api/me/intents`、`/{id}` | 游客 | 只看本人。别人的单按不存在返回 404 |
| GET | `/api/articles`、`/{id}` | 无 | 只返回 `PUBLISHED` |
| POST | `/api/merchant-applies` | 无 | 入驻申请，状态 `PENDING` |

列表数据是 `{items, total, page, pageSize}`，`page` 从 1 开始，默认 20 条。

排序：`comprehensive`（`home_sort`）、`price_asc`、`price_desc`。地区筛选包含下级地区。

意向：房源不存在 404；未上架 410；离店早于到店、到店早于今天 409；未登录 401。没有支付字段，请求里多出的金额字段会按未知字段拒绝。

门店详情的权威套餐文案是 `packageText`（对应 `store.package_text`）。`packages` 可以是空数组。表弟店同时有一条不同的 `store_package`，展示和验收仍以 `packageText` 为准。

`testData=true` 的上架房源仍会出现在列表和详情里，并带 `testTag=测试`。

### 缓存

`CacheAside` 缓存公开配置、地区、首页、上架门店详情。键前缀仍是 `njl:`。读 Redis 失败或超时则直接查库并记日志，不返回 5xx。写缓存失败也不影响本次读到的数据库结果。令牌继续只放内存。

详情在读缓存前先看当前上架状态，已下架不会把旧缓存当成可订。

### 种子

`V3__seed_featured_stores.sql` 只追加：

- 广陵「表弟店」（首页推荐，套餐文案「含双早与农家晚餐，可住两晚，周末不加价。」）
- 海陵河畔农家（首页推荐，没有套餐明细行）
- 崇川江边小院（上架，不进首页推荐）
- 标签「河鲜」「亲子」
- 已发布攻略「苏中周末住哪里」

这些店 `test_data=false`。脚本里没有抓取数据。

## ③ 改动点

| 对象 | 动作 |
|------|------|
| `SecurityConfig` | 游客注册登录公开；`/api/auth/me`、意向、`/api/me/**` 需要 `TOURIST` |
| `web.portal` / `service.portal` | 门户控制器与服务 |
| `CacheAside` | 热点读缓存，失败回源 |
| `StoreMapper.xml` | 上架列表筛选与排序 |
| `V3__seed_featured_stores.sql` | 手工精选店与攻略 |
| `app.cache.enabled` | 可关闭缓存（测试默认关掉 Redis 依赖） |
| 测试 | 注册登录、公开配置、410、意向隔离、令牌不串用、套餐主文案、缓存回源 |

## ④ 影响点

- 门户可以按上述路径联调。运营审核和商家接口仍未提供。
- 前端详情展示 `packageText`。`packages` 只作辅助，空列表是正常门店。
- 验收看 V3 里的表弟店等手工店。导入或抓取房源必须 `test_data=true`，门户会标「测试」。
- 本地没有 Redis 时，这些读接口仍然返回数据库结果。
- 游客令牌调用 `/api/admin/**` 得到 403，运营令牌不能提交意向或读取游客资料。
