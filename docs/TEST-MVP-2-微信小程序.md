# 测试用例 · MVP-2 微信小程序（v0.2）

| 项 | 内容 |
|----|------|
| 版本 | **v0.2** |
| 日期 | **2026-10-18**（Asia/Shanghai） |
| 维护 | 资深测试工程师 |
| 范围 | **MVP-2 微信小程序（W-B）**：游客端微信登录、首页 Banner、找店列表/详情、提交意向、我的意向、分享到门店、攻略只读；账号与 Web 打通；**无支付/定金/钱包**；**无**商家/运营小程序 |
| 对齐 | [PRODUCT-MVP-2-微信小程序-范围](./PRODUCT-MVP-2-微信小程序-范围.md) §5 **C-MP1～C-MP8** / **C-MP2b** / §2～§3 业务规则；分期见 [PRODUCT-MVP-分期-已拍](./PRODUCT-MVP-分期-已拍.md)；Banner 规则对齐 [PRODUCT-MVP-1.5-广告位-范围](./PRODUCT-MVP-1.5-广告位-范围.md)；页面 IA 见 [DESIGN-MVP-2-微信小程序](./DESIGN-MVP-2-微信小程序.md) §8 与 [UI-PAGES-MVP-2-微信小程序](./UI-PAGES-MVP-2-微信小程序.md) |
| 契约来源 | **均已合 main**：PRODUCT **api#28**；TEST 原 **api#29**（本文 v0.1）；DESIGN / UI-PAGES / UI 门禁 **api#30**；后端 TECH **api#32** [TECH-DESIGN-MVP-2-微信小程序](./TECH-DESIGN-MVP-2-微信小程序.md)；前端 TECH **web#14**（sister 仓 `nongjiale-web` 的 `docs/FE-TECH-DESIGN-MVP-2-微信小程序.md`，**不把全文拷进本仓**）。微信登录 / 打通 / `keyword` / 无手机号 403 以 **api#32** 为准。小程序页面 path 以 **web#14** 为准；与 DESIGN §8 冲突时以 FE TECH 为准 |
| 结论规则 | **仅已执行可下结论**；未跑一律标「待测」；发版/生产/删数前停等批准 |
| 硬锁 | **无支付 / 定金 / 收银台 / 钱包**；不改意向状态机码；`CHECKED_IN` 中文唯一 **「已入住」**（入+住；产品 C-MP8 / §3.7 与 TECH；禁已定着/已落实/已落地/已订立/已成交/已达成）；商家/运营配置入口小程序内不可达 |
| EXEC 锁 | **未解锁** — UI 门禁与两端 TECH 已合 main，**不挡**；现在只等 **业务实现合入** + **测服测试号 / 体验版可达**。正式 AppID **不挡**（体验版 AppID 即可；正式号以后只换环境变量） |
| 运维依赖 | 体验版预览依赖运维配置的测试号与合法域名；AppID / Secret 只来自 `WECHAT_MINIAPP_APP_ID` / `WECHAT_MINIAPP_SECRET`，本文与源码都不写真实值。本文件是用例，不代替运维清单 |
| 禁测 / 范围外 | 见 §不测；本包不做支付用例、商家端/运营端小程序、库存锁房、短信验证码 |

## 状态说明

| 状态 | 含义 |
|------|------|
| 待测 | 未执行（本版默认） |
| PASS / FAIL | 已执行结论 |
| 阻塞 | 依赖未就绪（业务实现未合入、测服体验版/测试号不可达） |
| 不测 | 明确不做 |

| 后端可测? | 含义 |
|-----------|------|
| **是** | 沿用已有 Web 对客/探活接口，可不靠小程序 UI；实现未合入也不改这些路径的现网行为 |
| **部分** | 路径已回填。现网可抽检的复用接口现在就能对；微信新接口与无手机号 403 要等实现合入后才存在，此前调用得到 404「接口不存在」，**不得 PASS，也不得把 404 当成产品 FAIL** |
| **否** | 纯小程序 UI / 微信分享 / 对客文案才能完整验 |

**方式：** API / UI / 人工（可组合）。

### 微信 / 账号（api#32，替换 v0.1「待 TECH」）

| 方法 | 路径 | 鉴权 | 要点 |
|------|------|------|------|
| POST | `/api/auth/wechat/login` | 公开 | body 仅 `code`（`wx.login` 的 code，trim 后非空，最长 128）。成功 `data`：`token`、`expiresAt`、`userId`、`role=TOURIST`、`displayName`、`phone`、`phoneBound`、`wechatBound`、`accountLinked`。响应无 `session_key` |
| POST | `/api/auth/wechat/bind` | 游客 Bearer，且令牌带 openid | 两种正文互斥：`phoneCode`（可无密码）或 `phone`（`^1\d{10}$`）+ `password`。成功体与登录相同，客户端必须改存新 `token`。冲突 **409**（见 C-MP2b-4） |
| POST | `/api/auth/wechat/unbind` | 同上 | 无请求体。解当前 AppID 的 openid；手机号、密码、意向保留。成功 `data` 同 link：`wechatBound=false`、`accountLinked=false`，无 `token` |
| GET | `/api/auth/wechat/link` | 游客 Bearer（网页版令牌也可读） | `userId`、`phone`（本人不脱敏，无则 null）、`phoneBound`、`wechatBound`、`accountLinked` |

打通主键：手机号把当前 openid 绑到已有手机号用户，之后两端都是同一个 `portal_user.id`。`accountLinked=true` 同时满足：该用户 `phone` 非空、`password_hash` 非空、且当前 AppID 下有这条 openid。未打通时 `GET /api/me/intents` **只含这个 `userId` 的单**，响应不增加「已从网页版同步」字段，也不把别人的单抄进来。绑定提交后下一次查询即可见，**不要发明同步延迟**。

无手机号（壳用户）调用 `POST /api/booking-intents` → **403**「请先绑定手机号，完成与网页版账号打通后再提交意向」，**不插入行**。对照：Web / 小程序**未登录**（无 Token 或过期）是 **401**「未登录」。已有手机号的网页游客提交结果与今天相同，不走这条 403。

### 页面路由

小程序页面 path 以 FE TECH web#14 为准；与 DESIGN §8 冲突时以 FE TECH 为准（攻略为 `guides/*`：列表 `/pages/guides/index`，详情 `/pages/guides/detail?id=`）。home / list / detail / login / intent / my / webview 与 FE TECH 一致。分享 query `id={storeId}`、首页 Tab、外链页，以及 Banner `linkUrl` 的打开方式，与后端 TECH api#32 写死项相同。FE TECH 全文不抄入本仓。

| 页面 | path / query |
|------|----------------|
| 首页 | `/pages/home/index`（Tab） |
| 找店 | `/pages/list/index`（Tab） |
| 门店详情 | `/pages/detail/index?id={storeId}`。`storeId` = `GET /api/stores/{id}` 的 `id`。UI 稿 `listingId` 就是这个字段 |
| 登录 / 绑定手机号 | `/pages/login/index`（微信登录与补全手机号同页） |
| 提交意向 | `/pages/intent/form?listingId={storeId}` |
| 意向成功 | `/pages/intent/success?id={intentId}` |
| 我的意向 | `/pages/my/intents`（Tab「我的」；未登录进登录页） |
| 意向详情 | `/pages/my/intent-detail?id={intentId}` |
| 攻略列表 | `/pages/guides/index`（首页入口，不进底栏） |
| 攻略详情 | `/pages/guides/detail?id={articleId}` |
| 外链容器 | `/pages/webview/index`（DESIGN query `url=`） |

**分享（C-MP7）：** `onShareAppMessage` 的 path = `/pages/detail/index?id={storeId}`。打开后再 `GET /api/stores/{id}`。410 展示「已下架或不可用」，隐藏提交。无分享后端接口、无计数。

**Banner 点击（C-MP4-2，TECH 对 `linkUrl` 的解释，接口字段不改）：**

| `linkUrl` | 小程序 |
|-----------|--------|
| `/` | 切到首页 Tab `/pages/home/index` |
| `/?city={slug}` | 切到找店 Tab；slug 用 `GET /api/regions` 换成 `regionId`。slug 不在 OPEN 市：提示「该目的地暂未开通」，不请求 CLOSED 列表 |
| `http://` 或 `https://` | 业务域名已配置则打开 `/pages/webview/index`；否则弹层复制链接（DESIGN：复制成功 Toast「已复制，请到浏览器粘贴打开」） |
| 其他以单个 `/` 开头 | Toast「暂无法在小程序内打开」。不白屏，不挡首页其他入口 |

**找店关键词：** `GET /api/stores` 可选 `keyword`。不传、或 trim 后空串，与现网一致。超过 64 字 → 400「参数不正确」。合法值在 OPEN 市 + 上架之上按店名包含匹配。关键词不能把 CLOSED 市的店带出来。

复用且契约零改动：`GET /api/home/banners`；`GET /api/regions`；`GET /api/stores/{id}`；`POST /api/booking-intents`（成功体不变）；`GET /api/me/intents`、`GET /api/me/intents/{id}`；`GET /api/articles`、`GET /api/articles/{id}`；`GET /api/config/public`；`GET /health`。网页对照：`POST /api/auth/login`、`GET /api/auth/me`。

### 已拍 / 产品定参数（验收必盯）

| 口径 | 验收落点 |
|------|----------|
| 包 = **W-B**（W-A + 首页 Banner + 攻略只读） | 全文件 |
| 仅游客小程序；商家/运营继续 Web | C-MP8-2；§不测 |
| 账号主体 `portal_user.id`；打通凭证是手机号 | C-MP2b |
| 地理：仅 **OPEN** 市上架供给；CLOSED 不串脏 | C-MP3 |
| Banner：启用 ALL 可见可点；停用后刷新消失；`sortOrder` 越小越前 | C-MP4 / C-MP5 |
| 攻略只读；无问答/专题等入口 | C-MP6 |
| 分享卡片落到该店详情，path 带 `id` | C-MP7 |
| 未登录提交须先登录（401）；无手机号提交 **403** 且不产生单 | C-MP1 |
| 成功页：单号 + 客服电话 + **意向 ≠ 已锁房**；「我的意向」可见 | C-MP2 |
| 全路径无支付/定金/钱包；CHECKED_IN 仅 **「已入住」** | C-MP8 |

---

## 前置总览（测服）

| 项 | 要求 |
|----|------|
| 基线 | 测服 Web 门户 / 运营 / 商家可达；意向状态机与 MVP-0/1 一致；开城与 Banner（MVP-1.5）能力可用 |
| 城市 | 至少一市 **OPEN** 且有上架可订店（建议扬州）；至少一市 **CLOSED** 可对照（建议南通）；CLOSED 不得对客泄漏 |
| Banner | 可配启用 **ALL** Banner ≥1；排序用例另需 ≥2 条启用且 `sortOrder` 不同（运营 Web 配置）。点击验收把链接配成 `/`、`/?city={slug}` 或 http(s) 之一 |
| 攻略 | 至少 1 篇已发布（`PUBLISHED`）可供列表/详情只读 |
| 客服电话 | 站配/成功页可展示的客服电话已配置（`GET /api/config/public` 的 `contactPhone`） |
| 账号 | 游客演示号见私密备忘（手机 `17606838922`）；运营 `17606838920`（Banner/开城配置）；商家 `17606838921`（仅确认小程序无商家入口，勿测商家写能力）。**口令见私密备忘，本文不写明文** |
| 小程序 | **测试号 / 体验版可开**即可进入正式 EXEC 的小程序侧；合法 request 域名已配（或开发者工具在约定范围内关闭校验，以运维为准）。**正式 AppID 不挡** |
| 实现 | 微信四个接口、无手机号 403、`keyword` 随业务实现 PR 合入后才在测服存在。未合入前相关用例阻塞，不对 404 下产品结论 |

---

## C-MP1 未登录提交意向 → 要求登录；登录后可完成

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP1-1** | 小程序游客**未登录**；测服有 OPEN 市上架可订店 | 找店 `/pages/list/index` → 详情 `/pages/detail/index?id={storeId}` → 点「提交意向」 | **要求登录**，进入 `/pages/login/index`；**未**产生新意向单。直调 API 无 Token：`POST /api/booking-intents` → **401**「未登录」（与 Web 未登录一致） | UI / API | 详情 `/pages/detail/index?id={storeId}`；登录 `/pages/login/index`；`POST /api/booking-intents` | 部分 | 待测 |
| **C-MP1-2** | C-MP1-1 已到登录页；测服已配体验版 AppID / Secret | `POST /api/auth/wechat/login`，body `{ "code": "<wx.login code>" }`。若响应 `phoneBound=false`，再 `POST /api/auth/wechat/bind`（`phoneCode`，或 `phone` + `password`）直到 `phoneBound=true`，换用返回的新 `token`，回到详情提交合法意向 | 登录成功 `role=TOURIST`；绑定后可完成提交；进入 `/pages/intent/success?id=`；产生有效意向 | UI / API | `POST /api/auth/wechat/login`；`POST /api/auth/wechat/bind`；`POST /api/booking-intents` | 部分 | 待测 |
| **C-MP1-3** | 微信登录已成功，但是壳用户：`phone=null`，`accountLinked=false` | 用该壳令牌 `POST /api/booking-intents`（合法日期/人数/联系人，无金额字段） | **403**「请先绑定手机号，完成与网页版账号打通后再提交意向」；**不产生单**。这不是未登录的 401 | API | `POST /api/booking-intents`；对照 `GET /api/auth/wechat/link` | 部分 | 待测 |

---

## C-MP2 已登录 + 可订店 → 提交意向成功页与「我的意向」

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP2-1** | 小程序游客已登录且 **`phoneBound=true`**；OPEN 市上架可订店 | 详情 `/pages/detail/index?id={storeId}` 进 `/pages/intent/form?listingId={storeId}`，填写合法日期/人数/联系人 → 提交 | 成功；**得单号** `intentNo`（Web 口径以 `NJL` 开头可对照）；初始状态 `PENDING`；进入成功页 | UI / API | `POST /api/booking-intents`；成功页 `/pages/intent/success?id={intentId}` | 部分 | 待测 |
| **C-MP2-2** | C-MP2-1 成功 | 查看成功页全文案与电话区 | 成功页展示**客服电话**（与 `GET /api/config/public` 的 `contactPhone` 一致）；文案含 **意向 ≠ 已锁房**；**无**支付/定金/锁房 CTA | UI | `/pages/intent/success?id={intentId}` | 否 | 待测 |
| **C-MP2-3** | C-MP2-1 成功；同一登录用户（同一 `userId`） | 打开「我的意向」列表，并打开该条详情 | **可见**刚提交的意向（单号/店名/状态可读）；只读；无取消/支付/改状态入口 | UI / API | `/pages/my/intents`；`/pages/my/intent-detail?id={intentId}`；`GET /api/me/intents`、`GET /api/me/intents/{id}` | 部分 | 待测 |

---

## C-MP2b Web ↔ 小程序账号打通

同一 `portal_user.id` 上没有同步任务。绑定事务提交后，另一次查询就能看见。

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP2b-1** | 网页版已有手机号 P、密码 W 的游客，且 P 名下已有至少一条意向。小程序可 `wx.login` | 1. `POST /api/auth/wechat/login` 得壳用户，`accountLinked=false`。2. 壳令牌 `GET /api/me/intents`。3. `POST /api/auth/wechat/bind`，手填 P+W，或微信手机号就是 P。4. 用响应里的**新** `token` 提交一条意向 | 绑定响应 `userId` 等于 P 的 `portal_user.id`，`accountLinked=true`，`phoneBound=true`，`wechatBound=true`。壳令牌的列表**不含** P 已有的单。绑定后提交得到 `intentNo`。网页 `POST /api/auth/login`（P、W）再 `GET /api/me/intents` **可见同一 `intentNo`** | UI / API | `POST /api/auth/wechat/login`；`POST /api/auth/wechat/bind`；`GET /api/auth/wechat/link`；`POST /api/booking-intents`；`GET /api/me/intents` | 部分 | 待测 |
| **C-MP2b-2** | C-MP2b-1 已是同一 `userId` | 网页版再提交一条意向；小程序用绑定后的令牌打开 `/pages/my/intents` | 可见网页这条 `intentNo`。两端状态码一致。若样本被运营跟进到 `CHECKED_IN`，接口仍返回码 `CHECKED_IN`，对客中文只显示「已入住」 | UI / API | `POST /api/booking-intents`；`GET /api/me/intents`；`/pages/my/intents` | 部分 | 待测 |
| **C-MP2b-3** | 另一手机号 Q 名下已有意向 `intentNo`。当前是未绑定的壳用户 | 壳令牌 `GET /api/auth/wechat/link`，再 `GET /api/me/intents` | `accountLinked=false`，`phoneBound=false`。列表**不含** Q 的 `intentNo`。页面若展示未打通提示，文案为「网页版意向同步即将开通，当前仅显示本端提交的意向」，不得写成已经同步 | API / UI | `GET /api/auth/wechat/link`；`GET /api/me/intents`；`/pages/my/intents` | 部分 | 待测 |
| **C-MP2b-4** | 已能登录并调用绑定 | 分两次（数据互相独立）：甲、当前微信已绑手机号 A，再绑另一个号 B。乙、目标手机号在**当前 AppID** 已有另一枚 openid，再拿新 openid 去绑该号 | 甲 **409**「当前微信已绑定其他手机号」。乙 **409**「该手机号已绑定其他微信」。两边行都不改。先解绑再登录新壳，才能改绑到另一个号 | API | `POST /api/auth/wechat/bind` | 部分 | 待测 |

---

## C-MP3 OPEN / CLOSED → 列表与目的地仅 OPEN；CLOSED 不串脏

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP3-1** | 测服有 OPEN 市上架店 + CLOSED 市（可对照南通） | 打开 `/pages/list/index`。API：`GET /api/stores` 不传 `keyword`；另传一个能命中店名的 `keyword`，以及指向 CLOSED 市的 `regionId` | **仅见 OPEN** 市上架供给。不传 `keyword` 与现网列表一致。`keyword` 只在店名包含匹配内过滤，**不能**带出 CLOSED 市的店。CLOSED 市：空列表，`destinationStatus=CLOSED`，`emptyHint`「该目的地暂未开通」 | UI / API | `/pages/list/index`；`GET /api/stores`；`GET /api/stores?keyword=`；`GET /api/regions` | 部分 | 待测 |
| **C-MP3-2** | 同上 | 查看首页/找店**目的地**（市选择器） | 目的地仅 **OPEN** 市；**不展示** CLOSED / 未开通市为可选目标 | UI / API | `GET /api/regions`；`/pages/home/index`、`/pages/list/index` | 部分 | 待测 |
| **C-MP3-3** | CLOSED 市曾有店/历史数据；或直链 CLOSED 市店 id | 列表筛 CLOSED（若 UI 仍露出）或 `regionId` 指向 CLOSED；可选打开已知 CLOSED 店详情 | **不串脏**。详情不可订口径对齐 Web：**410**「已下架或不可用」 | UI / API | `GET /api/stores?regionId=`；`GET /api/stores/{id}`；`/pages/detail/index?id=` | 部分 | 待测 |

---

## C-MP4 启用 ALL Banner → 首页可见可点；停用后刷新不再展示

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP4-1** | 运营 Web 已配至少 1 条**启用**、范围 **ALL**、有效图+链接的 Banner | 打开 `/pages/home/index` | 首页 **可见**该 Banner；0 条启用时整块不渲染，不挡找店/攻略 | UI / API | `GET /api/home/banners`；`/pages/home/index` | 部分 | 待测 |
| **C-MP4-2** | C-MP4-1 成功。分别准备：`/`、`/?city={OPEN slug}`、一条 http(s)、可选一条其他 `/` 路径 | 点击各 Banner | `/` 落到首页 Tab。`/?city=` 落到找店并带上对应 OPEN 市；slug 非 OPEN 时提示「该目的地暂未开通」。http(s) 在业务域名已配时打开 `/pages/webview/index`，否则弹层复制链接。其他 `/` 路径 Toast「暂无法在小程序内打开」，首页其余入口仍在。无整页白屏 | UI | `/pages/home/index`；`/pages/webview/index`；`/pages/list/index` | 否 | 待测 |
| **C-MP4-3** | C-MP4-1 所用 Banner 仍启用且首页可见 | 运营将该条**停用**；小程序首页刷新（重进或下拉） | 该 Banner **不再展示**；启用集合变空则整块隐藏，找店/攻略主路径仍正常 | UI / API | 启停沿用运营 Web；断言 `GET /api/home/banners` | 部分 | 待测 |

---

## C-MP5 ≥2 条启用不同 sort → 展示/轮播起始序越小越前

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP5-1** | ≥2 条**启用**且入展示集的 Banner；`sortOrder` 不同（例：A 较小、B 较大） | 打开 `/pages/home/index`；记录展示顺序或轮播**起始帧** | 顺序与配置一致：**`sortOrder` 越小越靠前**（A 在 B 前）。接口同序 | UI / API | `GET /api/home/banners`；`/pages/home/index` | 部分 | 待测 |
| **C-MP5-2** | 同上且条数 ≥2 | 观察首帧；滑动或等待自动切（约 5s，DESIGN §3.2） | 轮播**起始序**为 `sortOrder` 最前一条；指示点可用 | UI | `/pages/home/index` | 否 | 待测 |

---

## C-MP6 已发布攻略 → 列表/详情可读；无问答/专题等入口

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP6-1** | 测服有已发布攻略 ≥1 | 从首页入口打开攻略列表 | 列表**可读**；含已发布篇；不主推 CLOSED 市攻略（口径对齐 Web） | UI / API | `/pages/guides/index`；`GET /api/articles` | 部分 | 待测 |
| **C-MP6-2** | 知悉已发布攻略 id | 打开攻略详情 | 正文/标题可读；未发布或不存在维持现网 404 | UI / API | `/pages/guides/detail?id={articleId}`；`GET /api/articles/{id}` | 部分 | 待测 |
| **C-MP6-3** | 攻略列表/详情/相关导航可达 | 巡检入口与页内模块 | **无**问答、专题、社区帖等本包未做入口；仅只读攻略 | UI | `/pages/guides/index`；`/pages/guides/detail?id=` | 否 | 待测 |

---

## C-MP7 可订店 → 分享门店卡片并打开 → 落到该店详情

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP7-1** | OPEN 市上架可订店；体验版分享可用 | 在 `/pages/detail/index?id={storeId}` 发起分享 → 另一微信会话打开卡片 | 卡片 path 为 `/pages/detail/index?id={storeId}`（必须带该店 id）。打开后店名/图文与源店一致，数据对照 `GET /api/stores/{id}`。下架/未开通：**410**「已下架或不可用」，隐藏提交 | UI | 分享 path `/pages/detail/index?id={storeId}` | 否 | 待测 |

---

## C-MP8 全路径巡检 → 无支付/商家运营入口；CHECKED_IN 仅「已入住」

> 横切硬门禁。本包**不做**支付功能验收，只验「没有」。`CHECKED_IN` 对客中文**只有「已入住」**。禁止已定着、已落实、已落地、已订立、已成交、已达成。

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **C-MP8-1** | 主路径可达：`/pages/home/index`、`/pages/list/index`、`/pages/detail/index`、`/pages/intent/success`、`/pages/my/intents`、`/pages/guides/index`、`/pages/login/index` | 全文案与按钮巡检。可选：`POST /api/booking-intents` 塞 `amount` 或 `deposit` | **无**支付、定金、收银台、钱包、买位、积分等 CTA。未知字段 **400**「请求体格式不正确」 | UI / API | 上列页面；`POST /api/booking-intents` | 部分 | 待测 |
| **C-MP8-2** | 底栏与「我的」可达 | 巡检全部可点入口 | **无**商家工作台、运营后台、Banner/开城/攻略配置入口。底栏只有首页 / 找店 / 我的 | UI | `/pages/home/index`、`/pages/list/index`、`/pages/my/intents` | 否 | 待测 |
| **C-MP8-3** | 测服存在 `CHECKED_IN` 意向（运营 Web 跟进造数）；打通用户可在小程序「我的意向」见到 | 打开该意向列表与 `/pages/my/intent-detail?id=` | 状态中文**仅** **「已入住」**。接口码仍是 `CHECKED_IN`，不另发一套中文状态名 | UI | `GET /api/me/intents` / `/{id}`；`/pages/my/intents` | 否 | 待测 |

### C-MP8 冒烟矩阵（执行勾选）

| 路径 | 无支付/定金/钱包 | 无商家/运营配置入口 | CHECKED_IN=「已入住」 | 状态 |
|------|------------------|---------------------|----------------------|------|
| 首页 / Banner `/pages/home/index` | ☐ C-MP8-1 | ☐ C-MP8-2 | — | 待测 |
| 找店 / 详情 / 提交 / 成功页 | ☐ C-MP8-1 | ☐ C-MP8-2 | — | 待测 |
| 我的意向 `/pages/my/intents` | ☐ C-MP8-1 | ☐ C-MP8-2 | ☐ C-MP8-3 | 待测 |
| 攻略 `/pages/guides/index` | ☐ C-MP8-1 | ☐ C-MP8-2 | — | 待测 |
| 登录 / 绑定 `/pages/login/index` | ☐ C-MP8-1 | ☐ C-MP8-2 | — | 待测 |

---

## 冒烟 S1（本包最小集 · 尽量 API-first）

| ID | 前置 | 操作 | 期望 | 方式 | 接口或路径 | 后端可测? | 状态 |
|----|------|------|------|------|------------|-----------|------|
| **S1-HEALTH** | 测服基线 | 探活 | 服务 UP | API | `GET /health` | 是 | 待测 |
| **S1-LOGIN** | 体验版 AppID / Secret 已配；实现已合入。未合入则本条阻塞 | `POST /api/auth/wechat/login` body `{ "code" }`；再用返回 token `GET /api/auth/wechat/link`。网页对照仍可用 `POST /api/auth/login`、`GET /api/auth/me` | 得游客令牌，`role=TOURIST`，`wechatBound=true`。link 含 `accountLinked`、`phoneBound`、`wechatBound`、`phone`、`userId`。无 `session_key`。未配置微信 → 503「微信登录未配置」，且网页探活仍可用 | API / UI | `POST /api/auth/wechat/login`；`GET /api/auth/wechat/link`；登录页 `/pages/login/index` | 部分 | 待测 |
| **S1-HOME-BANNERS** | Banner 数据已配或允许空 | `GET /api/home/banners`；可选打开 `/pages/home/index` | 200；仅启用且合法范围条目；空数组时小程序隐藏 Banner 区 | API（+UI） | `GET /api/home/banners` | 是 | 待测 |
| **S1-STORES** | 至少一市 OPEN | `GET /api/regions` + `GET /api/stores`（OPEN 市 `regionId`，**不传** `keyword`）；再传一次短 `keyword`；抽检 `GET /api/stores/{id}` | 不传 `keyword` 与现网一致（现网即可抽检）。regions 仅 OPEN。`keyword` 只命中店名且不把 CLOSED 市店带出（该断言等实现合入）。详情可读 | API | `GET /api/regions`；`GET /api/stores`；`GET /api/stores?keyword=`；`GET /api/stores/{id}` | 部分 | 待测 |
| **S1-GUIDES** | 有已发布攻略 | `GET /api/articles`；抽检 `GET /api/articles/{id}` | 列表含已发布；详情可读 | API | `GET /api/articles`；`GET /api/articles/{id}` | 是 | 待测 |

---

## 不测（本包明确不做 / 范围外）

| 项 | 说明 |
|----|------|
| **支付 / 定金 / 收银台 / 钱包** | MVP-3；本包仅 C-MP8「无支付」巡检，**不发明**支付正向用例 |
| **商家端小程序 / 运营后台小程序** | 不做；配置仍 Web。无 `/api/mini/merchant`、`/api/mini/admin` |
| **库存日历 / 锁房** | 跨期禁 |
| **改 Web 意向状态机码或规则** | 只多端；状态码与 Web 一致 |
| **短信验证码** | TECH 不接短信。号码来自微信手机号组件，或手填网页版手机号 + 密码 |
| **App / 支付宝小程序** | MVP-4 / 未排 |
| **独立 IM** | 仍电话/客服配置 |
| **商家买位 / 曝光统计 / 分享计数** | 同 MVP-1.5 禁项；分享无后端接口 |
| **强制探测外链是否 200** | 对齐广告位产品口径；不挡展示 |
| **正式 AppID 切换** | 不挡本包 EXEC。换 AppID 后 openid 不同，用手机号再绑回原用户；本包不测刷 openid 脚本 |
| **unionid 合并** | 不做 |

---

## §执行顺序（建议）

| 序 | 阶段 | 内容 |
|----|------|------|
| 0 | **EXEC 锁** | 见下节。未解禁前本文件是用例，**不可**对未合入实现下 PASS |
| 1 | 实现与测服 | 业务实现 PR 合入（微信四个接口、无手机号 403、`keyword`）→ 测服测试号 / 体验版可开。正式 AppID 不挡 |
| 2 | 冒烟 | **S1-HEALTH** → **S1-LOGIN** → **S1-HOME-BANNERS** → **S1-STORES** → **S1-GUIDES** |
| 3 | 主验收 | **C-MP1** → **C-MP2** → **C-MP2b** → **C-MP3** → **C-MP4** → **C-MP5** → **C-MP6** → **C-MP7** |
| 4 | 发版前巡检 | **C-MP8** 矩阵全勾 |

---

## EXEC 锁（本版重要）

> **本版 v0.2：EXEC 仍未解锁。** 产品、TEST 草案、DESIGN/UI 门禁、后端 TECH、前端 TECH 都已合 main。路径回填 **不等于** 已授权跑测。

解锁 **EXEC / 正式跑测** 现在只等这两件同时成立：

1. **业务实现合入**（小程序工程 + 后端微信登录/绑定/解绑/link、无手机号提交 403、`GET /api/stores` 可选 `keyword`）。设计稿已在 main，本用例不代替实现。  
2. **测服测试号 / 体验版可达**（能完成 `wx.login`、打开分享卡片、请求打到测服 API）。**正式 AppID 不挡。**

此前可以做用例评审，以及 S1 里标「是」的现网抽检（探活、Banner、地区、不带新语义的列表、攻略）。**不可**对尚未合入的微信接口下 PASS，**不可**把「接口不存在」写成产品失败，**不可**发版结论。

---

## 风险与依赖

| 项 | 说明 |
|----|------|
| EXEC 未解锁 | 等业务实现合入 + 体验版/测试号可达。正式 AppID 不挡 |
| 实现未合入 | `POST /api/auth/wechat/*` 与 `GET /api/auth/wechat/link` 今天还不存在。404 只说明没发版，不是登录产品缺陷 |
| 账号打通 | 未打通时列表只属于当前 `userId`。C-MP2b-3 不得假绿。打通后无延迟队列 |
| 壳用户提交 | 无手机号必须 403 且行数不变。已有手机号的网页用户不受影响 |
| 绑定冲突 | 两种 409 都要数据不动。解绑不清手机号、不删意向 |
| CLOSED 串脏 | 列表、目的地、`keyword`、分享落地若漏出 CLOSED → FAIL C-MP3 / C-MP7 |
| Banner 空区 | 0 启用仍占白挡主路径 → FAIL C-MP4 |
| 排序方向 | 实现若「越大越前」→ FAIL C-MP5 |
| CHECKED_IN 文案 | 对客只允许 **「已入住」**。禁止已定着、已落实、已落地、已订立、已成交、已达成 |
| 运维 | 体验版不可开则 UI 全阻。源码与本文不出现真实 AppID / Secret |
| 演示账号 | 只引用私密备忘手机号；**本文不写口令明文** |
| 发版 | 停等批准；不删生产数据 |

---

## 用例统计（本版 v0.2）

| 分组 | 条数（含子用例） |
|------|------------------|
| C-MP1 未登录→登录提交 | 3 |
| C-MP2 已登录提交与我的意向 | 3 |
| C-MP2b Web↔小程序打通 | 4 |
| C-MP3 OPEN/CLOSED | 3 |
| C-MP4 Banner 启用/点击/停用 | 3 |
| C-MP5 排序/轮播起始 | 2 |
| C-MP6 攻略只读 | 3 |
| C-MP7 分享到门店 | 1 |
| C-MP8 硬锁巡检 | 3（+ 矩阵勾选，不另计） |
| 冒烟 S1-* | 5 |
| **合计** | **30** |

相对 v0.1 的 27 条，只加了契约落定后才能写的 3 条：C-MP1-3（无手机号 403）、C-MP2b-3（未打通不串单 + link）、C-MP2b-4（绑定 409）。`keyword` 写进 C-MP3-1 与 S1-STORES，不另编号。

### 后端可测分布（v0.2）

- **是（沿用现网 API-first）**：S1-HEALTH、S1-HOME-BANNERS、S1-GUIDES  
- **部分（路径已定；新微信行为或 `keyword` 等实现合入）**：C-MP1-*、C-MP2-1、C-MP2-3、C-MP2b-*、C-MP3-*、C-MP4-1、C-MP4-3、C-MP5-1、C-MP6-1、C-MP6-2、C-MP8-1、S1-LOGIN、S1-STORES  
- **否（纯 UI / 微信分享 / 文案）**：C-MP2-2、C-MP4-2、C-MP5-2、C-MP6-3、C-MP7-1、C-MP8-2、C-MP8-3  

### 仍阻塞正式 EXEC 的项

| 阻塞点 | 影响用例 |
|--------|----------|
| 业务实现未合入（微信四个接口、403、`keyword`） | C-MP1-2、C-MP1-3、C-MP2b-*、S1-LOGIN；`keyword` 新语义在 C-MP3-1 / S1-STORES |
| 测服测试号 / 体验版不可达 | 全部小程序 UI 与分享 |
| 正式 AppID | **不挡** |

---

## 交付说明（四段）

| 段 | 内容 |
|----|------|
| **概要** | MVP-2 微信小程序测试用例升到 **v0.2**：微信登录/绑定/解绑/link、无手机号 403、可选 `keyword`、小程序页面与分享 path 已按已合 main 的 TECH 回填。共 **30** 条，状态仍为待测。**EXEC 未解锁**。 |
| **详细** | 契约来源：PRODUCT api#28、TEST 原 api#29、DESIGN/UI api#30、后端 TECH api#32、前端 TECH web#14。打通后两端同一 `portal_user.id`；未打通列表只含本 userId。`CHECKED_IN` 只写「已入住」。正式跑测改等业务实现合入，以及测服测试号/体验版可达；正式 AppID 不挡。 |
| **改动点** | 只改本用例与 README 一行指针。v0.1 里微信登录、打通、页面路由、分享 path、Banner 外链上的「待 TECH」改为上表路径。攻略 path 按 FE TECH 改为 `/pages/guides/index`、`/pages/guides/detail?id=`（覆盖 DESIGN §8 的 `guide/*`）。新增 C-MP1-3、C-MP2b-3、C-MP2b-4。不改 Java、Flyway、控制器。 |
| **影响点** | 现网行为不变：微信接口尚未实现，本文不授权提前 PASS。发版仍须过 C-MP8。合并由项目经理转老板，本文不自行合并。 |

---

## 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-10-18 | 首版草案：对齐 PRODUCT-MVP-2（api#28）C-MP1～8 / C-MP2b；27 条；小程序路由与微信登录「待 TECH」；EXEC 未解锁 |
| v0.2 | 2026-10-18 | 路径回填：微信 `login/bind/unbind/link`、无手机号 403、`GET /api/stores` 可选 `keyword`、pages 与分享 path、Banner 外链。页面 path 以 FE TECH web#14 为准；攻略为 `/pages/guides/index`、`/pages/guides/detail?id=`（与 DESIGN §8 的 `guide/*` 冲突时取 FE TECH）。用例 27→30。EXEC 改为只等业务实现合入 + 测服测试号/体验版可达，正式 AppID 不挡。`CHECKED_IN` 禁已定着等异名 |
