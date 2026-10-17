# BE-7 开城交付

## ①概要

运营可以按市开通或关闭目的地。手勾只保留目的地数据、客服覆盖、脏数据清理三项；上架店数和攻略数按实时计数自动判断，不能手改。未达标不能开通。门户只露出已开通的市。关城后的店详情和创建意向沿用 410「已下架或不可用」。种子指标只给合计和分市三个数字，可导出 CSV。没有支付，没有定金，不改意向状态。

## ②详细

开通单位是 `RegionLevel.CITY`。`region.open_status` 为 `CLOSED` 或 `OPEN`，只对市有意义；空值在门户上按未开通。`enabled` 仍然表示节点能不能维护。

`region_open_checklist` 与市一对一，只存 `destination_ready`、`support_covered`、`dirty_data_cleared`，加上 `version`、`updated_at`、`updated_by`。不存供给达标和攻略达标。

门槛 N、M 放在 `site_config`：`open_city.min_stores` 默认 5，`open_city.min_articles` 默认 1。缺行、空串、非正整数时回退到 5 和 1。现有站点配置接口仍然只读写站名、主色和客服电话。

店数：上架、`test_data=false`、地区启用，且店挂在该市或该市下面的区县。攻略数：已发布，且挂在该市或下属区县。直接挂省的不计入任何市。

开通要求三项手勾都为真，并且店数 ≥ N、攻略数 ≥ M。否则 `POST .../open` 返回 409，中文缺项用「；」连接。已经开通再点开通返回「该市已开通」。关城没有门槛。计数后来跌破也不会自动关城。

清单 PATCH 只能改三个手勾，并必须带 `version`。请求体里出现 `supplyReady` 或 `articlesReady` 时返回 400「供给达标与攻略达标按实时计数自动勾选，不能手改」。版本不符返回 409「数据已被修改」。

门户 `GET /api/regions` 只返回已开通的市、这些市下仍启用的区县，以及下面还有开通市的省。房源列表增加 `destinationStatus` 和 `emptyHint`。未开通市返回空列表和「该目的地暂未开通」，不混入其他市。默认列表和首页推荐只保留已开通市里的上架房源。未开通市的攻略不进列表和首页，直链仍可打开。

指标按 `booking_intent.created_at` 滚动 7 天、30 天或全部。`confirmedCount` 只数当前 `CONFIRMED`，`checkedInCount` 只数当前 `CHECKED_IN`。不下钻。CSV 带 UTF-8 BOM，列是「市,意向数,已确认,已入住」。

扬州、泰州、南通在 V4 里标为 OPEN，三项手勾为真。当前种子店和攻略不够 N/M，详情里②③为未勾，但不会把这三市打回关闭，门户行为与 MVP-0 一致。

开通和关城在提交后失效 `CacheName.REGION` 的 `enabled`，以及首页 `default`。手勾不删这两把缓存。店详情在读缓存前判断所属市是否开通，所以关城不必逐店删详情缓存。

## ③改动点

- Flyway `V4__city_open.sql`：`region.open_status`、`region_open_checklist`、苏中三市 OPEN 与三项手勾、两条 `site_config`。
- 枚举 `RegionOpenStatus`、`DestinationStatus`、`MetricsWindow`；`SiteConfigKey` 增加两个键；审计增加 `CITY_CHECKLIST`、`CITY_OPEN`、`CITY_CLOSE` 和目标 `REGION`。
- 运营接口：城市列表、详情、清单 PATCH、开通、关城、指标汇总和 CSV。
- 门户：地区树、房源列表、首页、攻略列表按开通市过滤；详情和创建意向对未开通市返回 410。
- 文档：`docs/ADMIN-API.md`、`docs/TECH-DESIGN-MVP-1-开城.md`、`README.md`。

## ④影响点

- 新插入且未标 OPEN 的市，门户目的地、默认房源列表和首页推荐都看不见。种子三市仍是 OPEN，现有 Chip 和三家上架店还在。
- 未开通市的店详情和创建意向与下架共用 410「已下架或不可用」。运营后台仍可查看和编辑这些店。
- 未开通市的攻略不出现在列表和首页；知道 id 仍能打开已发布正文。
- 商家没有新接口。入驻仍然只看地区是否启用，不要求已开通。意向只读，没有支付字段。
- `CHECKED_IN` 文案仍是「已入住」。未知路径（含 `/api/payments`）仍是 404「接口不存在」。
- 关城不删除历史意向和门店。乐观锁冲突文案仍是「数据已被修改」。
- 开通或关城如果没有删掉 `njl:region:enabled`，门户最长会按地区缓存 TTL 显示旧目的地。本实现会在提交后删除该键和首页缓存。
