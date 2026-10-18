# 执行清单 · MVP-1.5 广告位（EXEC）

| 项 | 内容 |
|----|------|
| 版本 | **v0.2**（全量 · API + UI） |
| 日期 | **2026-10-18**（Asia/Shanghai） |
| 维护 | 资深测试工程师（Grok Bot EXEC） |
| 对齐用例 | [TEST-MVP-1.5-广告位.md](./TEST-MVP-1.5-广告位.md) **v0.2.1**（C-Ad1～5 + S1） |
| 契约 | api#22 Ready；测服 jar main **`7f4a353`（api#25）** + Flyway **V5**；静态 **`fb0f933`**（本跑冻结；api#26 已合 main **未**热滚） |
| EXEC 锁 | **已解锁（本跑）** —— 业务实现已合入 + 测服已滚 V5/banners；可对 Banner API/UI 下 PASS/FAIL |
| 测服 API | 优先 `http://127.0.0.1:18080`（本跑主用）；回落 `http://106.12.204.206:8080`（health/登录亦 UP） |
| UI Hosts | 门户 `http://127.0.0.1:14323`（游客）；运营 `http://127.0.0.1:14324`（运营）；商家本机直连 `:43125` Chrome `ERR_HTTP_RESPONSE_CODE_FAILURE`，改用本地转发 `http://127.0.0.1:14325` → staging `:43125`（API 改写至 `:18080`） |
| 账号 | 运营 `17606838920`；游客 `17606838922`；商家 `17606838921`（仅 C-Ad5-3）；口令 **见私密备忘**（本文不写明文） |
| 城市 | 扬州 **OPEN**（id=2）；南通 **CLOSED**（id=4）；泰州 OPEN（id=3，未改） |
| 结论规则 | **仅已执行可下结论**；发版/生产/删数前停等批准 |
| 本跑范围 | **API + UI 全量已跑**；截图见 `docs/exec-mvp15-ui/` |
| 清理 | 本跑创建带 `[EXEC-MVP15]` 标题的 Banner（id 3～7）已 **disable + DELETE**；种子 id=1/2 跑后已恢复：id=1 ALL `linkUrl=/` sort 10 enabled；id=2 CITY 扬州市 `linkUrl=/?city=yangzhou` sort 20 enabled（C-Ad1-3 测中曾暂改 id=2 外链 `https://example.com`，已还原） |

---

## 0. EXEC 解锁声明

| 条件 | 状态 |
|------|------|
| 路径回填（api#22） | ✓ |
| 产品门禁 PASS | ✓ |
| 老板 TECH 终验 | ✓ |
| 业务合入 + 测服滚 jar/静态（V5 + banners） | ✓ 本跑确认 `GET /health` UP；Admin/门户 Banner API 可用；UI 三端可达 |

→ **本文件为已授权执行清单；API + UI 结果可下 PASS/FAIL。**

---

## 1. 测服前置

| # | 检查项 | 结果 |
|---|--------|------|
| P1 | `GET /health` | **PASS** · `{"service":"nongjiale-api","status":"UP"}` HTTP 200（127.0.0.1:18080） |
| P2 | 运营登录 | **PASS** · `POST /api/admin/auth/login` → code=0，role=`OPERATOR`，得 Bearer（新口令已验证登录） |
| P3 | 游客登录 | **PASS** · `POST /api/auth/login` phone `17606838922` → code=0 得 Token |
| P4 | 商家登录（C-Ad5-3 预备） | **PASS** · `POST /api/merchant/auth/login` → code=0 得 Token；UI 商家登录 `17606838921` OK（经 14325 转发） |
| P5 | 扬州 OPEN / 南通 CLOSED | **PASS** · cities：扬州 OPEN、南通 CLOSED |
| P6 | Banner 表/种子 | **PASS** · Admin 列表见种子 id=1 ALL「苏中逛一逛」、id=2 CITY 扬州「扬州周末」 |

---

## 2. 建议执行顺序（本跑）

| 序 | 阶段 | 内容 | 本跑 |
|----|------|------|------|
| 1 | 冒烟 | S1-HEALTH → S1-ADMIN → S1-BANNER-ADMIN → S1-PORTAL-HOME | API ✓；UI ✓ |
| 2 | 主验收 | C-Ad1 → C-Ad2 → C-Ad3 → C-Ad4 | API ✓；UI ✓ |
| 3 | 发版巡检 | C-Ad5 三端 | UI ✓；API 侧商家无 Banner 路由已抽检 |

---

## 3. 按用例结果表

结果栏：`PASS` / `FAIL` / `SKIP` / `待测` + 简短证据。

### 3.1 冒烟 S1

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **S1-HEALTH** | API | 服务 UP | **PASS** | `GET /health` → HTTP **200** `status=UP` |
| **S1-ADMIN** | API | 运营 Token | **PASS** | `POST /api/admin/auth/login` → **200**/code=0，`role=OPERATOR`，`adminId=1`；新口令已验证登录 |
| **S1-BANNER-ADMIN** | API / UI | 列表可达 | **PASS** | API：`GET /api/admin/banners` → **200**/code=0，`items`+`total`。UI：运营侧真实路由 **`/banners`**「Banner 管理」（`/admin/banners` 显示「页面不存在」）；列表见种子；有新建/编辑/删除。截图 `docs/exec-mvp15-ui/admin-banners-list.png` |
| **S1-PORTAL-HOME** | API / UI | 门户 Banner 接口 + 首页区 | **PASS** | API：`GET /api/home/banners` → **200**/code=0，数组。UI：登录后门户首页可见 Banner 区（匿名 `/` 重定向登录）；轮播 + 上/下张 + 两圆点。截图 `docs/exec-mvp15-ui/portal-home-banner.png`、`portal-carousel.png` |

### 3.2 C-Ad1 启用全站可见可点

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **C-Ad1-1** | API / UI | 新建启用 ALL → 列表+门户可见 | **PASS** | API：`POST /api/admin/banners` body `title=[EXEC-MVP15] ALL test` `scope=ALL` `imageUrl=https://picsum.photos/800/300` `linkUrl=/` `sortOrder=5` `enabled=true` → **200** 得 **id=3**；门户含 id=3；另建外链 ALL **id=4**。UI 观感（首页 Banner 区可见）见 C-Ad5-1 / S1-PORTAL-HOME |
| **C-Ad1-2** | UI | 站内链接同 tab | **PASS** | ALL Banner id=1 已为 `<a class="home-banner-frame" href="/">`（router-link，无 `target`）。点击同 tab 停留门户首页 `http://127.0.0.1:14323/`，标题「首页 · 荷塘月色」。截图 `docs/exec-mvp15-ui/portal-home-banner.png` |
| **C-Ad1-3** | UI | 外链新 tab | **PASS** | 运营编辑后 slide 2 为 `<a href="https://example.com" target="_blank" rel="noopener noreferrer">`。点击新开 Example Domain；原 tab 仍停门户。测后 `linkUrl` 已还原 `/?city=yangzhou`。截图 `docs/exec-mvp15-ui/portal-external-newtab.png` |
| **C-Ad1-4** | API / UI | ALL 不受 Chip/目的地影响 | **PASS** | API：`GET /api/home/banners` 与 `?cityId=2` / `?citySlug=yangzhou` / `?city=yangzhou` / `?regionId=2` 均返回同一 ALL 集合（含 id=3/1/4）；**忽略**查询市参。UI 首页 Banner 区观感见 S1-PORTAL-HOME / C-Ad5-1 |

### 3.3 C-Ad2 停用 / 空隐藏

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **C-Ad2-1** | API / UI | 停用后门户不再展示 | **PASS** | API：`POST /api/admin/banners/4/disable` → **200** `enabled=false`；随后门户 ids 无 4。UI 侧启用展示见 C-Ad3-2 / S1-PORTAL-HOME（停用后不再出现的观感与 API 一致，本跑以 API 为主证） |
| **C-Ad2-2** | API / UI | 0 启用 → 空数组（FE 隐藏） | **PASS** | API：临时 disable 全部启用条后 `GET /api/home/banners` → `data=[]`；已恢复种子 1/2 enabled。FE 空态隐藏与「有启用则显示」对照见 S1-PORTAL-HOME（有 Banner 区） |

### 3.4 C-Ad3 排序越小越前

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **C-Ad3-1** | API / UI | sort 升序；越小越前 | **PASS** | API：建 id=5 `sortOrder=1`、id=6 `sortOrder=200`；门户顺序 `[(5,1),(3,5),(1,10),(2,20),(6,200)]` **升序**；Admin 列表同升序。UI 轮播起始=最前见 C-Ad3-2 |
| **C-Ad3-2** | UI | ≥2 轮播起始=最前 | **PASS** | 两启用 Banner；圆点「第 1 张 / 第 2 张」。首屏最低 sort「苏中逛一逛」(sort 10)，第1张 `aria-current`。手动下一张 →「扬州周末」(sort 20)。自动约 5s 进下一张（hover/focus 暂停）。截图 `docs/exec-mvp15-ui/portal-carousel.png` |

### 3.5 C-Ad4 按市 OPEN / CLOSED

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **C-Ad4-1** | API / UI | 扬州 CITY 启用入展示集 | **PASS** | API：`POST` CITY `cityId=2` → **id=7**；门户含 id=7 `scope=CITY` `citySlug=yangzhou`。UI 侧 CITY 条「扬州周末」出现在轮播见 C-Ad3-2 |
| **C-Ad4-2** | API / UI | 南通 CLOSED 定向不出现 | **PASS**（写路径门禁覆盖） | Admin **无法**写入 CLOSED 定向：`POST`/`PUT` CITY `cityId=4` → HTTP **400**「投放市未开通，请改选全站或其他已开通市」；本跑未做 DB 直插历史脏数据。门户无可展示南通定向。若需验「历史脏行过滤」须运维直插后再复测 |
| **C-Ad4-3** | API / UI | CLOSED 不可有效新建定向 | **PASS** | 同上 **400**；PUT 改 id=7→南通亦 **400**；合法 PUT 回扬州 **200**。运营表单侧字段见 C-Ad5-2（投放范围等，无绕过） |

### 3.6 C-Ad5 无支付 / 商家入口

| ID | 方式 | 期望要点 | 结果 | 证据摘要 |
|----|------|----------|------|----------|
| **C-Ad5-1** | UI | 门户无支付/买位文案 | **PASS** | 门户首页：无「支付/充值/押金/钱包/买广告/竞价/广告位/推广」CTA；无 CHECKED_IN/banned 近义；无「广告」角标。Banner 图（example.com）未加载 → 标题回退（非角标）。截图 `docs/exec-mvp15-ui/portal-home-banner.png` |
| **C-Ad5-2** | UI | 运营 Banner 页无买位/计费 | **PASS** | 导航「广告位」→ `/banners`「Banner 管理」（列表见种子；新建/编辑/删除）。创建/编辑仅「图片地址/跳转链接/投放范围/排序/启用」。无「买广告/竞价/计费/支付/统计看板」。注：`/admin/banners`「页面不存在」，真实路由 `/banners`；侧栏另有「种子看板」（非 Banner 控制）。截图 `docs/exec-mvp15-ui/admin-banners-list.png` |
| **C-Ad5-3** | UI | 商家无广告配置入口 | **PASS** | API：商家登录成功；`GET /api/admin/banners` → **403**；`/api/merchant/banners|ads` → **404**。UI：商家 `/stores`、`/intents` 导航仅「门店 + 本店意向」；文案「意向由平台客服跟进」；无「广告位/Banner/买位/推广竞价」。截图 `docs/exec-mvp15-ui/merchant-nav.png` |

### C-Ad5 冒烟矩阵

| 端 | 无支付/买位 | 无统计看板 | 已入住文案 | 无广告配置入口 | 状态 |
|----|-------------|------------|------------|----------------|------|
| 门户 | ☑ C-Ad5-1 | ☑ | ☑ | — | **PASS** |
| 运营广告配置 | ☑ C-Ad5-2 | ☑ | — | —（有 Banner 配置、无买位） | **PASS** |
| 商家 | ☑ C-Ad5-3 | — | — | ☑（API 无路由 + UI 无入口） | **PASS** |

---

## 4. 本跑创建的测试 Banner（已清理）

| id | title | scope | sortOrder | 终态 |
|----|-------|-------|-----------|------|
| 3 | `[EXEC-MVP15] ALL test` | ALL | 5 | DELETED |
| 4 | `[EXEC-MVP15] ALL ext` | ALL | 50 | DELETED |
| 5 | `[EXEC-MVP15] sort-front` | ALL | 1 | DELETED |
| 6 | `[EXEC-MVP15] sort-back` | ALL | 200 | DELETED |
| 7 | `[EXEC-MVP15] CITY yangzhou` | CITY/扬州 | 15 | DELETED |

终态门户样本：`GET /api/home/banners` → `[{id:1,title:苏中逛一逛,sortOrder:10,scope:ALL,linkUrl:/},{id:2,title:扬州周末,sortOrder:20,scope:CITY,citySlug:yangzhou,linkUrl:/?city=yangzhou}]`（均 enabled）。

---

## 5. UI 证据与截图

| 项 | 内容 |
|----|------|
| 门户 | `http://127.0.0.1:14323`（游客 `17606838922`） |
| 运营 | `http://127.0.0.1:14324`（运营 `17606838920`）；Banner 管理路由 **`/banners`**（非 `/admin/banners`） |
| 商家 | 本机 `:43125` 失败；转发 `http://127.0.0.1:14325` → staging `:43125`（API → `:18080`）；登录 `17606838921` OK |
| 截图目录 | `docs/exec-mvp15-ui/` |

| 文件 | 覆盖 |
|------|------|
| `portal-home-banner.png` | C-Ad1-2 / C-Ad5-1 / S1-PORTAL-HOME |
| `portal-carousel.png` | C-Ad3-2 / S1-PORTAL-HOME |
| `portal-external-newtab.png` | C-Ad1-3 |
| `admin-banners-list.png` | C-Ad5-2 / S1-BANNER-ADMIN |
| `merchant-nav.png` | C-Ad5-3 |

---

## 6. 计数（本跑全量 · API + UI）

| 结论 | 条数 | 说明 |
|------|------|------|
| **PASS** | **18** | S1-HEALTH、S1-ADMIN、S1-BANNER-ADMIN、S1-PORTAL-HOME、C-Ad1-1、C-Ad1-2、C-Ad1-3、C-Ad1-4、C-Ad2-1、C-Ad2-2、C-Ad3-1、C-Ad3-2、C-Ad4-1、C-Ad4-2、C-Ad4-3、C-Ad5-1、C-Ad5-2、C-Ad5-3 |
| **FAIL** | **0** | — |
| **待测 / SKIP** | **0** | — |
| **合计用例** | **18** | 与 TEST v0.2.1 一致 |
| **阻塞** | **0** | — |

> 纯 UI：C-Ad1-2、C-Ad1-3、C-Ad3-2、C-Ad5-1、C-Ad5-2、C-Ad5-3 均 **PASS**。双端用例均有 API + UI 证据（或 UI 观感交叉引用）。

---

## 7. 差异 / 风险留痕

| 项 | 说明 |
|----|------|
| C-Ad4-2 历史脏数据 | 写路径已拒 CLOSED，无法经 API 造「启用但 CLOSED 市」行；门户过滤未单独用脏行验证 |
| 查询参数 | 门户 `GET /api/home/banners` **不**按 city 查询参数过滤 ALL（符合产品） |
| 运营路由 | 真实 Banner 管理为 **`/banners`**；`/admin/banners` 为「页面不存在」（文档/导航以实测为准） |
| 商家静态入口 | 直连 staging `:43125` Chrome 失败；本跑经本地 `:14325` 转发完成，不改变生产/测服部署 |
| 冻结 | jar **`7f4a353`（api#25）** + V5；静态 **`fb0f933`**。api#26 已合 main **但本 EXEC 未热滚** |
| 种子数据 | id=1/2 为 V5 种子，本跑未删；测中曾短暂全停用 / id=2 外链暂改后均已恢复 |
| 密码 | 明文仅私密备忘；本文写「新口令已验证登录」 |

---

## 8. 交付说明

| 段 | 内容 |
|----|------|
| **概要** | MVP-1.5 广告位 EXEC **v0.2**：EXEC 已解锁；**PASS 18 / FAIL 0 / 待测 0 / 阻塞 0**；测服冻结 `7f4a353`+V5 / 静态 `fb0f933`（api#26 未热滚）。 |
| **详细** | 覆盖 health、三端登录、Admin CRUD/启停、门户列表、站内同 tab、外链新 tab、轮播/auto-advance、ALL 不受市参、停用与空数组、sort 升序、扬州 CITY 入集、南通 CLOSED 拒写、C-Ad5 三端无支付/买位/商家广告入口。 |
| **改动点** | 文档补全 UI；无对产品/契约/生产改动。测试数据已清理。无 commit/PR。 |
| **影响点** | EXEC 技术侧全绿；**下一步：C-Ad1～5 产品复核**。 |

---

## 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-10-18 | 首跑 EXEC：解锁；API 部分全量；UI 待测；账号新手机号；口令见私密备忘 |
| v0.2 | 2026-10-18 | 补全 UI：六纯 UI + S1 双端 UI；计数 18 PASS / 0 FAIL / 0 待测；截图入库；冻结注明 api#26 未热滚 |
