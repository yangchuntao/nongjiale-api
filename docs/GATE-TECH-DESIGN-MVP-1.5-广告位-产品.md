# 产品门禁 · 技术设计 MVP-1.5 广告位（后端）

> 资深产品经理 · 2026-10-18  
> 审：`docs/TECH-DESIGN-MVP-1.5-广告位.md`（Origin api PR #22，`cursor/tech-design-mvp-1-5-ads-0d52`）  
> 对照：`PRODUCT-MVP-1.5-广告位-范围.md` · `DESIGN-MVP-1.5-广告位.md` · `UI-PAGES-MVP-1.5-广告位.md` · `GATE-UI-MVP-1.5-广告位-产品.md` · web PR #11 FE TECH

---

## 结论：**通过（PASS）**

契约与已拍范围、UI 门禁锁一致：`home_banner` 仅运营可写；门户 `GET /api/home/banners` 只返回启用且（ALL 或 CITY 且市仍 OPEN）的有序集合；ALL 不吃 Chip；CLOSED 不定向且关城后不对客；排序 `sort_order` 越小越前；0 条空数组由前端折叠；无支付/买位/曝光计费表与字段；商家无新接口；`CHECKED_IN` 仍「已入住」。四件套齐全。本 PR 仅文档，不挡项目经理请老板 TECH 终验；**业务代码 / V5 SQL 仍等老板终验后**。

---

## 抽检对照

| 锁 / 验收 | BE TECH 落点 | 门禁 |
|-----------|--------------|------|
| A★ 仅运营投放 + 仅首页 Banner | ①：表 `home_banner`；公开仅首页 GET；商家无新路由 | ✅ |
| 可配图/链/启停/排序/ALL·CITY | 运营 CRUD + `enable`/`disable`；字段齐；图片 URL | ✅ |
| 无支付 / 无买位 / 无曝光统计 | 「不做」：无 `impression_count`/`click_count`、无价格订单、无 `/api/payments` 新开 | ✅ |
| 商家无广告配置入口 | 商家包不新增；商家令牌打管理接口 403 | ✅ |
| ALL 忽略 Chip（Q3） | 公开 GET **不接收**城市参数；不得据此过滤 | ✅ |
| CITY 仅 OPEN；CLOSED 不定向 | 写入拒非 OPEN；公开入集用 `CityRules.isOpen`；关城不改 `enabled` | ✅ |
| 排序越小越前 | `sort_order ASC, id ASC`；新建默认 100 | ✅ |
| 0 / 1 / ≥2 | 接口只给有序集合；0→`data:[]`；轮播/静帧归前端 | ✅ |
| 链接站内 `/` 或 http(s) | 写入形态校验；不探测可达性 | ✅ |
| 无有效条目不报错 | 公开 200 + 空数组；不塞 feed 缓存 | ✅ |
| 不改 CHECKED_IN=已入住 | 现状表 +「不做」明确不改枚举与文案 | ✅ |
| C-Ad1～C-Ad5 | 种子建议 + C-Ad4 关城路径说明；实现后测覆盖 | ✅ |
| Admin / 门户真实 API 路径 | 公开 `GET /api/home/banners`；运营 `/api/admin/banners` 列表/详情/POST/PUT/DELETE + `POST …/enable|disable` | ✅ |
| 四件套 | ①概要 · ②详细 · ③改动点 · ④影响点 | ✅ |
| 本 PR 范围 | 仅 TECH 文档；无 Java / Flyway / 测试 | ✅ |

---

## 侧记（不挡 PASS）

| # | 说明 | 处置 |
|---|------|------|
| S1 | 管理接口 `OPERATOR` 与 `AGENT` 均可（与现网开城管理一致）；产品「仅运营」按排除游客/商家理解 | 与现网一致；商家端仍无入口 |
| S2 | 本包不做上传，只收 `imageUrl`（Q2 工程选定） | 满足「至少一种可用」 |
| S3 | V5 种子不插南通定向、不改南通 OPEN；C-Ad4 靠用例关城或另建 CLOSED 市 | 测试步骤写清即可 |
| S4 | 种子站内链示例 `/?city=yangzhou` 与门户现用 `regionId` 筛选参数可能不一致 | 占位链；验收改真实可点路径，不挡契约 |
| S5 | 与 FE TECH §2.4 草案差异：启停动词（POST enable/disable vs PATCH）、公开 `data` 形状（数组 vs `{items}`）、字段名 `sortOrder` | **以本 BE TECH 为契约源**；FE 实现前改草案 |

无阻塞性缺口。无 FAIL 项。

---

## 改动点 / 影响点（产品视角）

| 类型 | 说明 |
|------|------|
| 改动 | 无产品规则增改；门禁确认后端契约可终验 |
| 影响 | 终验后实现将新增 V5 表与上述接口；首页多一次公开 GET；关城后 CITY 条立即出集；测试按 C-Ad1～5 |
| 不改 | 支付/定金、意向状态机与「已入住」、开城 N/M 与苏中 OPEN 种子、商家只读、feed 缓存结构 |

---

## 下一步

1. **项目经理**：1:1 请老板技术设计终验（标题/开头加「老板」）。  
2. **后端**：终验通过前不写业务代码 / 不交 V5 SQL。  
3. FE 按本契约收口 §2.4 后实现；产品可选把本 GATE 合入 api `docs/`。
