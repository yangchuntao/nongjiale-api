# MVP-2 微信小程序 · 技术设计（W-B）

> 设计稿。待产品门禁 + 老板 TECH 终验后再写业务代码。  
> 本文件只定契约。不改 Java、Flyway、控制器或测试。  
> 范围、验收、页面与门禁见：
>
> - `docs/PRODUCT-MVP-2-微信小程序-范围.md`（C-MP1～C-MP8、C-MP2b；老板 W-B）
> - `docs/PRODUCT-MVP-分期-已拍.md`（本包 = MVP-2，插在广告位后、定金前）
> - `docs/DESIGN-MVP-2-微信小程序.md`（已入库；三 Tab、外链、登录留位、分享）
> - `docs/UI-PAGES-MVP-2-微信小程序.md`（已入库；MP-HOME～MP-WEBVIEW）
> - `docs/GATE-UI-MVP-2-微信小程序-产品.md`（UI 门禁 PASS，只作参考，本文件不另写 GATE）
> - `docs/TEST-MVP-2-微信小程序.md`（v0.1；路径当时标「待 TECH」，以本文件回填）
>
> 前提已在 `main`：开城 `docs/TECH-DESIGN-MVP-1-开城.md`，广告位 `docs/TECH-DESIGN-MVP-1.5-广告位.md`。最新迁移是 `V5__home_banner.sql`。

已锁参数：只做游客微信小程序后端。微信登录后发与网页版相同的游客 Bearer。账号主体是 `portal_user.id`。打通凭证是手机号。未打通时「我的意向」只含该 `userId` 自己的单。首页 Banner、OPEN 市供给、攻略、意向状态机全部复用现有门户接口。无支付。无商家小程序、无运营小程序。AppID / Secret 只来自环境变量。`CHECKED_IN` 文案仍是「已入住」（U+5DF2 U+5165 U+4F4F）。

Q3 在本文拍板，见「账号主体」。

## ①概要设计

小程序不新开一套门店、意向、Banner、攻略。浏览器门户继续走 `http://localhost:43123`，接口进程仍是 `8080`。小程序运行在微信里，请求打到同一套 HTTPS API；浏览器 CORS 名单不为此增加来源。合法域名由运维以后配置，不在本设计里写死域名。

新增的只有游客身份：

1. `wx.login` 的 `code` 换 `openid`（`jscode2session`）。
2. 用 `openid` 找到或创建 `portal_user`，再走现有 `TokenStore.issue`，角色 `TOURIST`。
3. 用微信手机号凭证或「手机号 + 密码」把这条微信身份绑到已有手机号用户上。绑完之后，小程序和网页版的「我的意向」都按同一个 `user_id` 读 `booking_intent`。

网页版登录响应、意向请求体、Banner / 地区 / 攻略路径保持原样。找店列表增加一个可省略的 `keyword`，不传时与今天一致。无手机号的微信壳用户调用 `POST /api/booking-intents` 得到 403，不产生单。这是 V6 之后才会出现的新状态，已有手机号的游客行为不变。

令牌不是 JWT。现网 Bearer 是 32 字节随机数的十六进制（64 字符），放在进程内存，TTL 取 `app.security.token-ttl`（12 小时），禁止写入 Redis。小程序登录复用这一条，不并行做第二套会话。

数据模型的下一版 Flyway 是 **V6**（只在实现 PR 提交）。新增 `portal_wechat_identity`。`portal_user.phone` 与 `password_hash` 改为可空，以便尚未打通的壳用户落库。不建支付表。`CHECKED_IN` 的码和标签「已入住」保持不动。

## ②详细设计

### 现状（按代码，实现时沿用）

| 事实 | 落点 |
|------|------|
| 技术栈 | Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、Redis 只做缓存、Bearer。枚举走码，不写魔法字符串 |
| 迁移 | 最新是 `V5__home_banner.sql`。本包下一版是 **V6**。本设计不提交 SQL |
| 游客 | 表 `portal_user`。`phone`、`password_hash` 今天都是 NOT NULL。`uk_portal_user_phone` 唯一。没有 openid 列 |
| 网页登录 | `POST /api/auth/register`、`POST /api/auth/login`。密码 BCrypt，6～64 位。成功体 `token, expiresAt, userId, role, displayName`。`role` 为 `TOURIST` |
| 当前用户 | `GET /api/auth/me` 为 `userId, phone, displayName, role`。未登录 401「未登录」。错角色 403「无权限」 |
| 令牌 | `InMemoryTokenStore`。`TokenStore` 注释写明禁止写入 Redis。前缀 `Bearer `。过期后再访问与未登录相同 |
| 意向归属 | `booking_intent.user_id` → `portal_user.id`。`GET /api/me/intents` 只按这个 id 过滤。他人的单详情 404「意向不存在」 |
| 意向创建 | `POST /api/booking-intents` 须 `TOURIST`。未知 JSON 字段 400「请求体格式不正确」。没有金额字段。初始状态 `PENDING`。单号 `intentNo` |
| 状态机 | `IntentStatus`：`PENDING / CONFIRMED / RESCHEDULED / CHECKED_IN / CLOSED`。`CHECKED_IN` 标签「已入住」。迁移只在运营 Web。本包不改枚举、不改标签、不加状态 |
| 门店列表 | `GET /api/stores` 查询参数是 `regionId, tag, priceMin, priceMax, sort, page, pageSize`。没有关键词。只含 OPEN 市的上架店。CLOSED 市得到空列表，`destinationStatus=CLOSED`，`emptyHint` 为「该目的地暂未开通」 |
| 门店详情 | `GET /api/stores/{id}`。不存在 404「房源不存在」。下架或市未开通 410「已下架或不可用」 |
| 地区 | `GET /api/regions` 只留 OPEN 市及其启用区县。元素含 `id, slug, name, level`。`level` 市为 `CITY` |
| Banner | `GET /api/home/banners` 公开、无城市参数、不缓存。启用，且（`ALL` 或 `CITY` 且该市仍 OPEN），`sortOrder` 升序再按 `id`。0 条是 `[]` |
| 首页 feed | `GET /api/home/feed` 缓存 5 分钟。推荐店已按 OPEN 过滤。小程序首页不依赖它 |
| 攻略 | `GET /api/articles`、`GET /api/articles/{id}` 只给 `PUBLISHED`。列表不占未开通市的名额。没有问答、专题接口 |
| 客服电话 | `GET /api/config/public`：`siteName, brandColor, contactPhone`。主色种子 `#4BA3D9` |
| 运营 / 商家 | `/api/admin/**`、`/api/merchant/**` 仍只在 Web。游客令牌访问它们是 403 |
| 未知路径 | 含 `/api/payments`，404「接口不存在」 |
| 成功信封 | `{code:0, message:"ok", data}`。失败 `code` 等于 HTTP 状态，`data` 为 null，`message` 中文 |
| 时钟 | `Asia/Shanghai`。时间 JSON 为 ISO-8601 |
| 覆盖率 | `jacoco.line.minimum` = 0.85。见 `docs/COVERAGE.md` |

### 复用与新增

| 能力 | 路径 | 本包 |
|------|------|------|
| 首页 Banner | `GET /api/home/banners` | 复用，契约零改动。点击解释在小程序里做，见下文 |
| 目的地 | `GET /api/regions` | 复用，契约零改动。市筛只用 `level=CITY` |
| 找店 | `GET /api/stores` | 复用。**唯一的兼容增量**：可省略查询参数 `keyword`。不传则与现网相同 |
| 门店详情 / 分享落地 | `GET /api/stores/{id}` | 复用，契约零改动。分享无后端接口 |
| 首页推荐（可选） | `GET /api/home/feed` | 复用，契约零改动。UI 标明无则不做，不挡验收 |
| 攻略列表 / 详情 | `GET /api/articles`、`GET /api/articles/{id}` | 复用，契约零改动 |
| 客服电话、站名、主色 | `GET /api/config/public` | 复用，契约零改动 |
| 提交意向 | `POST /api/booking-intents` | 复用。请求体与成功体不变。无手机号壳用户 403，不插入行 |
| 我的意向 | `GET /api/me/intents`、`GET /api/me/intents/{id}` | 复用，契约零改动。仍只按令牌里的 `userId` |
| 网页版注册登录 | `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` | 复用。成功字段不变。壳用户的 `me.phone` 可以为 null |
| 微信登录 | `POST /api/auth/wechat/login` | **新** |
| 绑定 | `POST /api/auth/wechat/bind` | **新** |
| 解绑 | `POST /api/auth/wechat/unbind` | **新** |
| 打通状态 | `GET /api/auth/wechat/link` | **新**。前端用它决定是否展示未打通提示，不改意向列表的字段 |
| 支付、商家端、运营端 | 无新路径 | 不做 |

### 账号主体（Q3 拍板）

产品允许「手机号或统一 userId」二选一。本文两种都用，职责分开：

| 概念 | 选定 | 原因 |
|------|------|------|
| 用户主体 | `portal_user.id` | 意向已经挂在 `booking_intent.user_id`。两端可见 = 同一个 id，不按 openid 再查一次 |
| 打通凭证 | 手机号 | 网页版今天只用手机号 + 密码。微信侧没有这个手机号之前，不能把两边说成同一个人 |
| 微信登录凭证 | `(app_id, openid)` | openid 随 AppID 变化。unionid 只有绑了开放平台才有，体验版经常没有，本包不拿它做合并 |
| 已打通 | `accountLinked=true` | 同时满足：该用户 `phone` 非空、`password_hash` 非空、且当前配置的 AppID 下有这条 openid 的绑定 |

`accountLinked=false` 时：

- `GET /api/me/intents` 只返回**这个** `userId` 的行。壳用户通常是空列表。
- 响应里不增加「已从网页版同步」之类的字段，也不把别人的单抄进来。
- 小程序根据 `GET /api/auth/wechat/link` 展示 DESIGN 已写的提示：「网页版意向同步即将开通，当前仅显示本端提交的意向」。
- `POST /api/booking-intents` 在 `phone` 为空时 403，不产生单。因此壳用户不会攒下一堆日后要偷偷合并的意向。

已有手机号的网页版用户不经过这条 403。他们没有微信绑定时 `accountLinked` 仍是 false，但意向创建保持今天的行为。小程序在完成绑定、令牌换成该 `userId` 之前，看不到这些单。

不接短信。UI 留的「手机号 + 验证码」不采用。号码来自微信手机号组件，或用户填写的网页版手机号；密码沿用注册规则（6～64 位，BCrypt），用来对齐网页版登录。

### 微信登录

小程序 `wx.login` 拿到一次性 `code`，立刻调用登录接口。`code` 只能用一次。

**POST `/api/auth/wechat/login`**

公开，与 `POST /api/auth/login` 一样不需要 Bearer。

请求体：

| 字段 | 规则 |
|------|------|
| `code` | 必填，trim 后非空，最长 128。这是 `wx.login` 的 code，不是手机号 code |

服务端在配置齐全时请求：

`GET https://api.weixin.qq.com/sns/jscode2session?appid&secret&js_code&grant_type=authorization_code`

超时 3 秒。只访问 `https://api.weixin.qq.com`。`session_key` 不落库、不写日志、不返回给客户端。本包不用它解密旧版 `encryptedData`。

成功（无 `errcode`，或 `errcode=0`）且带 `openid`：

1. 用**当前配置的 AppID** + `openid` 查 `portal_wechat_identity`。
2. 查到：加载对应 `portal_user`，签发游客令牌。`unionid` 若本次有值且库内为空，补写上去；不据此改绑用户。
3. 查不到：插入 `phone=NULL`、`password_hash=NULL`、`display_name=微信用户` 的壳用户，再插入身份行，签发令牌。
4. 并发下唯一键冲突：丢掉刚插入的空壳（若还没有意向、没有身份），改为沿用已存在的那一行签发。

令牌记录在内存里多记两个内部字段，不出现在 JSON：`loginChannel=WECHAT`，`wechatOpenid`。网页版密码登录的令牌这两个字段为空。解绑只作废带这个 openid 的令牌，避免把网页版会话一起踢掉。

成功 `data`：

| 字段 | 说明 |
|------|------|
| `token` | 与网页版相同形态的 Bearer 原文，不含 `Bearer ` 前缀 |
| `expiresAt` | 与 `TokenStore` 相同 |
| `userId` | `portal_user.id` |
| `role` | `TOURIST` |
| `displayName` | |
| `phone` | 未绑定为 null |
| `phoneBound` | `phone` 非空 |
| `wechatBound` | 登录成功时为 true |
| `accountLinked` | 见上节定义 |

失败不返回微信原始 `errmsg`（可能夹带敏感上下文）。句子只从枚举 `WechatMessage` 出。

### 绑定、解绑与冲突

绑定证明「当前这枚 openid」和「这个手机号」是同一个人。openid 来自**本次微信登录令牌**，不来自请求体。网页版密码登录的令牌没有 openid，不能调用绑定。

**POST `/api/auth/wechat/bind`**

须 `TOURIST`。两种正文互斥，多字段或少字段都是 400「请求体格式不正确」或下表更具体的句子。未知字段 400「请求体格式不正确」。

| 模式 | 正文 | 何时用 |
|------|------|--------|
| 微信手机号 | `phoneCode` 必填，最长 128。`password` 可空 | 按钮 `getPhoneNumber` 的 code。服务端再调微信换纯手机号 |
| 网页版手机号 | `phone` 必填，`^1\d{10}$`。`password` 必填 | 微信绑定的号码和网页版注册号不一致时，用密码证明拥有该网页账号；新号则用这次密码开通网页登录 |

`phoneCode` 模式调用：

`POST https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=...`  
正文 `{ "code": "<phoneCode>" }`。

`access_token` 用 `client_credential` 向 `https://api.weixin.qq.com/cgi-bin/token` 换取，进程内存缓存到 `expires_in - 120` 秒。不写入 Redis，不返回给客户端，日志不打 Secret、token、`session_key`。只接受国家码 86（缺省按 86）且 `purePhoneNumber` 匹配 `^1\d{10}$`。

`phoneCode` 一次性。新号码却没带合法密码时，接口 400，该 code 已经失效，客户端要重新授权。实现不要把「待绑定手机号」放进 Redis 或另一张队列表。

处理顺序，命中即返回，同一事务里完成写入：

1. 配置缺失 → 503，且不发起微信请求（`phoneCode` 模式）。手填手机号模式同样先看配置：绑定行要写入当前 AppID，AppID 为空就不能绑。
2. 令牌不是微信渠道（没有 openid）→ 400「请先使用微信登录」。
3. 当前用户已有手机号，且与目标号不同 → 409「当前微信已绑定其他手机号」。先解绑再登录新的壳，才能改绑到另一个号。解绑不清手机号、不删意向。
4. 目标号就是当前用户的手机号 → 200，幂等。已打通则 `accountLinked=true`。不改密码。若带了密码且与库内不一致 → 401「手机号或密码错误」，绑定关系不变。
5. 目标号还没有 `portal_user`：`password` 缺失或长度不是 6～64 → 400「请设置网页版登录密码」。通过则把手机号和 BCrypt 写到**当前壳用户**。`userId` 不变。
6. 目标号已有另一名用户，且该用户在**同一个 AppID** 下已有不同 openid → 409「该手机号已绑定其他微信」。两边数据都不改。
7. 目标号已有用户，证明不足：手填模式密码不匹配，或 `phoneCode` 模式带了密码但不匹配 → 401「手机号或密码错误」。`phoneCode` 模式不带密码时，微信已经证明号码，允许继续。
8. 当前壳上已有 `booking_intent` → 409「未打通账号下已有意向，不能合并」。正常路径走不到（无手机号不能提交）。禁止把壳上的单改挂到目标用户上，以免把两个人的单并成一份「已同步」。
9. 通过后：把身份行的 `portal_user_id` 改到目标用户。目标用户在其他 AppID 下的旧行保留（体验版换成正式版时靠这个并存）。壳用户若已无身份、无意向，删除壳。不覆盖目标用户的手机号、密码、昵称。作废内存里 `wechatOpenid` 等于这枚 openid 的令牌。签发**目标 userId** 的新微信令牌。网页版已发出的密码登录令牌保留。

成功 `data` 与登录成功体相同。`userId` 在第 9 步会变成网页版那个用户。客户端必须改存新 `token`。此后两边的「我的意向」都读这个 id。

**POST `/api/auth/wechat/unbind`**

须微信渠道的 `TOURIST`。无请求体。删除当前 AppID + 该 openid 的身份行。手机号、密码、意向保留。作废带该 openid 的内存令牌（含这一次请求的令牌）。密码登录令牌保留。

已无绑定 → 200，不报错。成功 `data` 与 `GET /api/auth/wechat/link` 相同：`wechatBound=false`，`accountLinked=false`。没有 `token`。客户端丢掉本地令牌。验收解绑以**重新 `wx.login` 后的新会话**为准：新会话是新壳，列表里没有解绑前那个手机号用户的单。

**GET `/api/auth/wechat/link`**

须 `TOURIST`（网页版令牌也可以读，用来显示「还没绑微信」）。`data`：

| 字段 | 说明 |
|------|------|
| `userId` | |
| `phone` | 没有则为 null。返回给本人，不脱敏 |
| `phoneBound` | |
| `wechatBound` | 当前配置 AppID 下，该用户是否有身份行 |
| `accountLinked` | |

网页版令牌没有 openid 时，`wechatBound` 只看表，不看令牌。

### 两端怎么验（C-MP2b）

同一 `portal_user.id` 上没有同步任务，也没有延迟队列。绑定事务提交后，另一次查询就能看见。测试不要发明「几秒后才同步」。

**小程序 → 网页**

1. 网页版用手机号 P、密码 W 注册（或使用已有账号）。
2. 小程序登录，得到壳用户，`accountLinked=false`。用壳令牌 `GET /api/me/intents`，结果里没有 P 的单。
3. `POST /api/auth/wechat/bind`，手填 P + W（或微信手机号就是 P）。响应 `userId` 等于 P 的 id，`accountLinked=true`。
4. 用新令牌 `POST /api/booking-intents`，得到 `intentNo`。
5. 网页版 `POST /api/auth/login` 用 P、W，再 `GET /api/me/intents`，可见同一 `intentNo`。

**网页 → 小程序**

1. 上一步已经是同一 id。
2. 网页版再提交一条，得到另一个 `intentNo`。
3. 小程序用绑定后的令牌 `GET /api/me/intents`，可见这一条。
4. 状态码与网页版一致。若样本被运营跟进到 `CHECKED_IN`，接口仍返回码 `CHECKED_IN`；中文标签仍是「已入住」。本包不在接口里另发一套中文状态名。

**未打通的反例**

另一个手机号 Q 名下已有意向。壳用户的列表接口不得包含 Q 的 `intentNo`。`accountLinked` 不得为 true。

### 配置

键名：

```yaml
wechat:
  miniapp:
    app-id: ${WECHAT_MINIAPP_APP_ID:}
    secret: ${WECHAT_MINIAPP_SECRET:}
```

实现时用 `@ConfigurationProperties(prefix = "wechat.miniapp")`，不要写进 `AppProperties` 的 `app` 前缀，避免和 CORS、令牌 TTL 混在一起。`application.yml` 只保留空默认值，让本地不配微信也能启动网页版。

| 规则 | 说明 |
|------|------|
| 禁止写死 | 源码、YAML、测试夹具、日志里都不放真实 AppID 或 Secret。测试用假值或桩 |
| 空串 | trim 后 AppID 或 Secret 任一为空，即视为未配置 |
| 未配置 | 登录、绑定、解绑、换手机号、换 `access_token` 全部 503「微信登录未配置」，且不发起出站请求 |
| 启动 | 不因为没配微信而拒绝启动。网页版、探活、门户读取保持可用 |
| 体验版 | 老板要求先用微信试用 / 体验 AppID。正式 AppID 以后只换 `WECHAT_MINIAPP_APP_ID` 与 `WECHAT_MINIAPP_SECRET` |
| 换 AppID | 正式号的 openid 与体验版不同。旧行留在表里（`app_id` 不同）。用户重新登录，再用手机号 + 密码绑回原 `portal_user`。本包不写刷 openid 的脚本 |

出站只有 `api.weixin.qq.com` 这三个调用：`jscode2session`、`cgi-bin/token`、`getuserphonenumber`。用 JDK `java.net.http.HttpClient`，不新增微信 SDK 依赖。`js_code` / `phoneCode` 失败不自动重试（凭证已作废）。

### 数据模型（V6 提案，本 PR 不提交）

H2 `MODE=MySQL` 与 MySQL 8 都要能跑。不使用部分唯一索引，不使用数据库 CHECK。互斥与「一名用户在一个 AppID 下只有一枚 openid」由唯一键 + 服务层一起保证。

**改 `portal_user`**

| 列 | 变更 |
|----|------|
| `phone` | 改为 NULL。唯一索引保留。MySQL 与 H2 的 UNIQUE 都允许多个 NULL，壳用户才能并存。禁止用空串占位 |
| `password_hash` | 改为 NULL。网页版登录若命中空密码，返回 401「手机号或密码错误」，不要 NPE |

已有行的手机号和密码不变。不回填假 openid。

**新表 `portal_wechat_identity`**

| 列 | 类型 | 说明 |
|----|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | |
| `portal_user_id` | BIGINT NOT NULL，FK → `portal_user.id` | |
| `app_id` | VARCHAR(32) NOT NULL | 绑定时的小程序 AppID，不是开放平台号 |
| `openid` | VARCHAR(64) NOT NULL | |
| `unionid` | VARCHAR(64) NULL | 仅保存。不参与查找用户、不参与合并 |
| `created_at` | TIMESTAMP NOT NULL | |

唯一键：

| 键 | 作用 |
|----|------|
| `(app_id, openid)` | 一枚 openid 在一个小程序里只属于一名用户 |
| `(portal_user_id, app_id)` | 同一用户在同一个 AppID 下只有一条绑定。另一个 AppID 可以再有一条，供体验版切正式版 |

不增加 `session_key`、支付字段、`subscribe` 消息表、分享计数表。解绑是物理删除该行，不做软删标记。

**枚举**

- `WechatMessage`：HTTP 状态 + 中文句子。服务只抛枚举常量。
- 展示名常量「微信用户」放在同一处，服务里不写这四个字的字面量。
- 不新增意向状态，不改 `IntentStatus.CHECKED_IN` 的码与标签「已入住」。
- 不新增 `ENABLED` 一类字符串来表示是否打通。打通是布尔计算，不是库内魔法字符串。

本包不扩展运营审计。绑定是游客自助，不写 `audit_log`。

### 新接口一览

| 方法 | 路径 | 鉴权 | 作用 |
|------|------|------|------|
| POST | `/api/auth/wechat/login` | 公开 | code 换游客 Bearer |
| POST | `/api/auth/wechat/bind` | `TOURIST`，且令牌带 openid | 把当前微信绑到手机号主体 |
| POST | `/api/auth/wechat/unbind` | `TOURIST`，且令牌带 openid | 只解除当前 AppID 的 openid |
| GET | `/api/auth/wechat/link` | `TOURIST` | 读打通布尔，供未同步提示 |

`SecurityConfig` 增加：`POST /api/auth/wechat/login` 为 `permitAll`；其余三条 `hasAuthority(TOURIST)`。商家令牌、运营令牌 403「无权限」。未登录 401「未登录」。

### 复用接口（契约保持）

下列路径的路径、方法、已有字段、错误句保持不变。小程序直接调用。

| 方法 | 路径 | 小程序用法 |
|------|------|------------|
| GET | `/api/config/public` | 首页客服电话、成功页电话、站名。主色仍读 `brandColor` |
| GET | `/api/regions` | 市筛。只用返回结果，不在客户端再拼 CLOSED 市 |
| GET | `/api/home/banners` | 首页 Banner。失败则前端隐藏该区，与 UI 一致。本接口继续不收城市参数、不进 feed 缓存 |
| GET | `/api/home/feed` | 仅当首页要画可选推荐卡时调用。不挡 C-MP |
| GET | `/api/stores/{id}` | 详情与分享打开后的数据。410 / 404 文案沿用 |
| GET | `/api/articles` | 攻略列表 |
| GET | `/api/articles/{id}` | 攻略详情。不存在或未发布维持现网 404 |
| POST | `/api/booking-intents` | 已有手机号才可提交。成功体仍是现有 `IntentResponse` |
| GET | `/api/me/intents` | 分页参数与现网相同。元素字段不增不减 |
| GET | `/api/me/intents/{id}` | 非本人 404「意向不存在」 |
| POST | `/api/auth/login` | 绑定完成后的网页方向验收 |
| POST | `/api/auth/register` | 预备网页账号。本包不改校验句 |
| GET | `/api/auth/me` | 字段仍是四个。壳用户 `phone` 为 null |

`POST /api/booking-intents` 的新增失败只针对 `phone IS NULL`：403「请先绑定手机号，完成与网页版账号打通后再提交意向」。不插入 `booking_intent`。请求体仍拒绝未知字段（含 `amount`、`deposit`）为 400「请求体格式不正确」。

### 找店关键词（唯一兼容增量）

UI 要求市 + 关键词。列表在服务端分页，不能改成前端过滤当前页。

`GET /api/stores` 增加可选查询参数 `keyword`：

| 规则 | 说明 |
|------|------|
| 省略或不传 | 过滤条件与今天相同，响应字段不变 |
| trim 后空串 | 当成省略 |
| 超过 64 字 | 400「参数不正确」 |
| 合法值 | 在现有 OPEN 市 + 上架条件之上，`store.name` 包含匹配 |
| 通配符 | 实现时转义 `%` 与 `_`，避免用户输入变成 LIKE 通配 |
| CLOSED | 仍走 `CityVisibility.listScope`。关键词不能把 CLOSED 市的店带出来 |
| 缓存 | 列表今天不缓存。不要把带关键词的结果写进 `STORE_DETAIL` |

不新增 `/api/mini/stores`。标签、价格、排序参数保留给网页，小程序本包可以不传。

### Banner 链接（接口不改，前端按此解释）

`linkUrl` 仍是运营在网页后台填的站内路径或 http(s)。不增加 `miniPath`，不改 `HomeBannerItem`。

| `linkUrl` | 小程序 |
|-----------|--------|
| `/` | 切到首页 Tab `/pages/home/index` |
| `/?city={slug}` | 切到找店 Tab，slug 用 `GET /api/regions` 换成 `regionId`。slug 不在 OPEN 市里：提示「该目的地暂未开通」，不请求 CLOSED 列表 |
| `http://` 或 `https://` | 业务域名已配置则打开 `/pages/webview/index`；否则弹层复制链接。与 DESIGN §3.2 一致 |
| 其他以单个 `/` 开头的路径 | Toast「暂无法在小程序内打开」。不白屏，不挡首页其他入口 |

C-MP4 验收时，运营把链接配成上表前三行之一。种子里已有 `/` 与 `/?city=yangzhou`，可以直接用。

### 分享

无新接口，无签名接口，无计数。小程序详情页 `onShareAppMessage`：

| 项 | 值 |
|----|-----|
| path | `/pages/detail/index?id={storeId}` |
| id | 等于 `GET /api/stores/{id}` 的 `id`。UI 稿里的 `listingId` 就是这个字段 |
| 打开后 | 再请求 `GET /api/stores/{id}`。410 展示「已下架或不可用」，隐藏提交 |

朋友圈分享不挡 C-MP7。

### 安全与路由

| 路径 | 谁可以访问 |
|------|------------|
| `POST /api/auth/wechat/login` | 任何人 |
| `POST /api/auth/wechat/bind`、`/unbind` | 带 openid 的游客 Bearer |
| `GET /api/auth/wechat/link` | 游客 Bearer |
| `POST /api/booking-intents`、`/api/me/**`、`GET /api/auth/me` | 游客 Bearer（保持今天） |
| `GET` 门户读接口（regions、stores、banners、articles、config、feed、health） | 公开（保持今天） |
| `/api/admin/**`、`/api/merchant/**` | 与今天相同。小程序不调用，也不新增平行路径 |
| `/api/payments` 及任何未注册路径 | 404「接口不存在」 |

Bearer 仍不进 Cookie。`allowCredentials` 保持 false。CORS 允许来源仍是 `app.cors-allowed-origins`：`http://localhost:43123`（门户）、`43124`（商家）、`43125`（运营）。小程序不是浏览器页面，不把微信来源加进这份名单。

进程端口保持 `server.port=8080`。门户页面仍是 43123。小程序请求的是 API 的 HTTPS 域名；开发者工具在域名配好之前可以关「校验合法域名」。本设计不写生产域名，也不改端口。

Redis 继续只做现有缓存（地区、feed、门店详情、站配）。微信 `access_token`、Bearer、`session_key` 都不进 Redis。

### 错误码与文案

HTTP `code` 仍是数字状态。不另造 `WECHAT_*` 业务码。

| HTTP | 场景 | message |
|------|------|---------|
| 400 | 登录没带 `code` | 请提供微信登录凭证 |
| 400 | 绑定正文对不上两种模式、未知字段、坏 JSON | 请求体格式不正确 |
| 400 | 手填手机号不匹配 `^1\d{10}$`，或微信返回的号码不是大陆 11 位 | 手机号格式不正确 |
| 400 | 新手机号没有 6～64 位密码 | 请设置网页版登录密码 |
| 400 | 密码长度不对（手填模式或新号） | 密码长度为 6 到 64 位 |
| 400 | 网页版令牌来绑定或解绑 | 请先使用微信登录 |
| 400 | `keyword` 超长、分页非法沿用现网 | 参数不正确 / 分页参数不正确 |
| 401 | 未登录、令牌过期、解绑后继续用这枚微信令牌 | 未登录 |
| 401 | `jscode2session` 或手机号 code 无效、已使用（含微信 40029、40163） | 微信登录已失效，请重试 |
| 401 | 手填密码不对，或幂等确认时密码不对 | 手机号或密码错误 |
| 403 | 商家或运营令牌 | 无权限 |
| 403 | 壳用户提交意向 | 请先绑定手机号，完成与网页版账号打通后再提交意向 |
| 409 | 当前微信已有另一个手机号 | 当前微信已绑定其他手机号 |
| 409 | 目标手机号在本 AppID 已有别的 openid | 该手机号已绑定其他微信 |
| 409 | 壳上竟已有意向 | 未打通账号下已有意向，不能合并 |
| 502 | 微信超时、网络失败、`errcode=-1`、HTTP 5xx | 微信服务暂不可用 |
| 503 | AppID 或 Secret 为空 | 微信登录未配置 |
| 410 | 下架或市未开通的详情 | 已下架或不可用（现网，不改） |

微信 40013（AppID 无效）视为配置错误：503「微信登录未配置」，日志只记 errcode，不记 Secret。

### 缓存

| 动作 | 处理 |
|------|------|
| 登录、绑定、解绑 | 不删地区、feed、门店详情、站配、Banner（Banner 本来不缓存） |
| 关键词搜索 | 不新做缓存键 |
| 开城 | 保持今天 |

### 测试（实现阶段，本 PR 不写）

JaCoCo 行覆盖率仍 ≥ 85%（`jacoco.line.minimum`）。新服务、微信客户端分支、绑定冲突都要进 `mvn test`。出站微信用桩，不访问公网。

单测至少覆盖：

| 项 | 断言 |
|----|------|
| 登录成功 | 桩返回 openid，签发的主体是 `TOURIST`，响应无 `session_key` |
| 再次登录同一 openid | 同一个 `userId`，不再生一个壳 |
| 未配置 | 503，桩未被调用 |
| 微信 40029 | 401「微信登录已失效，请重试」 |
| 新号绑定缺密码 | 400「请设置网页版登录密码」，手机号仍为空 |
| 新号绑定带密码 | `phone` 与 `password_hash` 写上，`accountLinked=true`，网页版可用该密码登录 |
| 手填已有手机号且密码正确 | 令牌 `userId` 变为原网页用户；壳删除；网页用户原意向仍在 |
| 该手机号已有本 AppID 的另一 openid | 409，两边行数不变 |
| 当前用户已绑其他手机号 | 409，不改绑 |
| 密码错误 | 401，不改绑 |
| 解绑 | 身份行没了；再用原微信令牌访问为 401；同一用户的密码登录令牌仍可看意向 |
| 换 AppID | 新 AppID 的 openid 可再绑到同一手机号用户，旧 AppID 行还在 |
| 壳用户提交意向 | 403，表行数不变 |
| 关键词省略 | 列表与未加参数时一致 |
| 关键词 | 只命中店名；CLOSED 市仍为空，`emptyHint` 仍是「该目的地暂未开通」 |
| 文案硬锁 | `IntentStatus.CHECKED_IN.label()` 等于「已入住」 |

集成至少覆盖 C-MP2b 两个方向，以及未打通时列表不含另一用户的 `intentNo`。顺带断言 `GET /api/payments` 仍是 404，商家令牌调绑定为 403。

`docs/TEST-MVP-2-微信小程序.md` 里标「待 TECH」的登录与打通，实现合入后由测试改用例路径。本 PR 不改那份用例，避免两份契约分叉。路径以上表为准。

### 实现时建议落点（仍不在本 PR 写代码）

- 配置：`WechatMiniappProperties`。`application.yml` 空默认。环境变量 `WECHAT_MINIAPP_APP_ID`、`WECHAT_MINIAPP_SECRET`。
- 枚举：`WechatMessage`。
- 表与实体：V6、`PortalWechatIdentity`。`PortalUser` 的手机号与密码允许 null。
- 客户端：`WechatMiniappClient`（三个 HTTPS 调用 + 内存里的 `access_token`）。
- 服务：登录、绑定、解绑、打通布尔。意向创建前读手机号是否为空。
- 令牌：`TokenStore` 能按 openid 作废微信渠道令牌。不写 Redis。
- 控制器：挂在 `/api/auth/wechat/**`。`SecurityConfig` 只加上述匹配。
- 列表：`StoreSearch` 增加可选 `keyword`，两条 SQL 共用。
- 不改 `IntentStatus`、`HomeFeedService` 的响应、Banner 服务、商家包、运营包。
- 实现阶段再补 `README` 的登录路径说明。本文件是契约源。

### 不做

支付、定金、收银台、钱包、买位、`/api/payments`。商家小程序与运营小程序，含任何 `/api/mini/merchant`、`/api/mini/admin`。短信验证码。微信用户资料、昵称头像解密、订阅消息、客服消息。unionid 合并。把体验版 openid 洗成正式版的脚本。分享统计。Banner 新字段。改意向状态机。把 `CHECKED_IN` 的标签改成「已入住」以外的字（禁止：已定着、已落实、已落地、已订立、已成交、已达成）。改 CORS 端口。把现有 Bearer 换成 JWT。把令牌或 `session_key` 放进 Redis。

### 假设（已拍板的工程选择）

1. 主体是 `portal_user.id`，打通凭证是手机号，微信凭证是 `(app_id, openid)`。
2. 不接短信。新手机号必须带 6～64 位密码，网页版才能登录。已有账号用密码或微信手机号凭证证明。
3. 无手机号不能提交意向。列表接口不改字段，打通布尔只在微信登录响应和 `/api/auth/wechat/link`。
4. 令牌继续是进程内随机 Bearer，不是 JWT。
5. AppID / Secret 只来自环境变量。没配则 503，进程仍能起。
6. 正式 AppID 只做配置替换。数据模型允许同一用户按 AppID 多一行，不另做迁移步骤。
7. 找店关键词是 `GET /api/stores` 的可选参数，不传则行为不变。
8. Banner 与分享不新增后端字段和接口。
9. 体验版先用。源码不出现真实 AppID。

## ③改动点

**本 PR（设计稿）**

- 新增 `docs/TECH-DESIGN-MVP-2-微信小程序.md`。
- `README.md` 增加一行指针，指向这份设计稿。
- 不改 `src/`、`src/main/resources/db/migration`、测试、`pom.xml`、`docs/ADMIN-API.md`、`docs/TEST-MVP-2-微信小程序.md`。
- 不新增 GATE 文档。UI 门禁已在 `docs/GATE-UI-MVP-2-微信小程序-产品.md`。

**终验通过后的实现（本 PR 不做）**

- V6：`portal_user.phone`、`password_hash` 可空；新建 `portal_wechat_identity`。
- 四个 `/api/auth/wechat/**` 接口，以及无手机号提交意向时的 403。
- `GET /api/stores` 可选 `keyword`。
- 内存令牌可按 openid 作废。
- 微信出站客户端与配置项。
- 单测与集成覆盖登录、绑定冲突、C-MP2b 两个方向、未打通不串单、`CHECKED_IN` 标签。JaCoCo 行覆盖率仍 ≥ 85%。

## ④影响点

- 网页版游客：注册、登录、意向请求与成功体不变。已有手机号的用户提交意向的结果不变。`GET /api/auth/me` 的字段不变；只有壳用户会出现 `phone=null`，网页版注册用户不会。
- 小程序：登录后若 `accountLinked=false`，意向列表只代表当前 `userId`，页面按 DESIGN 展示未打通提示。绑定成功并换用新令牌后，与网页版看到同一批 `intentNo`。
- 绑定冲突：一枚 openid 不能从已绑手机号上直接改绑；一个手机号在同一个 AppID 下不能挂两枚 openid。两种都是 409，数据保持原样。
- 解绑：该 openid 的小程序令牌失效。同一用户在网页版的密码令牌仍可看自己的意向。重新微信登录得到新壳，看不到原手机号名下的单，直到再次绑定。
- 供给与内容：Banner、地区、详情、攻略、客服电话的路径与字段不动。找店多一个可省略的 `keyword`。CLOSED 市仍然进不了列表。
- 分享：只约定页面 path 使用 `storeId`。后端不计数。
- 端口与跨域：API `8080`，门户来源 `43123`。小程序走 HTTPS 与微信合法域名，不改 CORS 名单。
- 配置：不配 AppID / Secret 时，只有微信四个接口与手机号凭证交换失败，返回「微信登录未配置」。其余接口可用。
- 意向文案：`CHECKED_IN` 继续是「已入住」。状态码不改。全站仍无支付路径。
- 覆盖率：实现阶段新代码纳入现有 JaCoCo。本 PR 无 Java 改动，不跑 `mvn test`。
- 依赖：依赖现有游客 Bearer、`portal_user`、意向、开城可见性、Banner 与攻略只读。不依赖 MVP-3 定金。
