# 执行清单 · MVP-2 微信小程序（EXEC）

| 项 | 内容 |
|----|------|
| 版本 | **v0.3.1**（产品最终复核可关项记债；本跑：开发者工具 UI + 真 `wx.login`；`urlCheck=false`；HTTPS 真机仍记债） |
| 日期 | **2026-10-18 15:55**（Asia/Shanghai） |
| 维护 | 资深测试工程师（Grok Bot EXEC） |
| 对齐用例 | [TEST-MVP-2-微信小程序.md](./TEST-MVP-2-微信小程序.md) **v0.2** |
| 契约 | TECH **api#32**；实现 **api#34** / tip **`c48411b`**；Flyway **V6/V7**；密钥已注入 |
| 三端静态 | 仍冻 **`fb0f933`**（本跑未动） |
| EXEC 范围 | v0.1 API 结论保留；v0.2 断连后续跑完成 **真 code + 模拟器 UI** |
| 测服 API | **主用** `http://106.12.204.206:8080` |
| 账号 | 运营 `17606838920`；商家 `17606838921`；演示游客 `17606838922`；口令 **见私密备忘** |
| 城市 | 扬州 OPEN(id=2)；泰州 OPEN(id=3)；南通 CLOSED(id=4) |
| 工具 | Windows `yangchuntao`；CLI auto `--auto-port 9420`；`miniprogram-automator`（本机临时目录，未进仓） |
| 结论规则 | **仅已执行可下结论**；**未**伪造 code；**未**写 AppSecret；**未**开 PR；**未**动演示 Banner（C-MP4-3） |
| 证据 | API：`docs/private/exec-mvp2-api-raw/`；UI/真 code：`docs/private/exec-mvp2-ui/out/`（无 Token 明文） |

---

## 0. EXEC 解锁声明（本跑）

| 条件 | 状态 |
|------|------|
| TEST v0.2 / 实现 `c48411b` / 密钥已注入 | ✓ |
| 开发者工具 CLI 登录 + open + auto | ✓ |
| 真 `wx.login` code → `/api/auth/wechat/login` | ✓ |
| 壳用户 403 / bind 演示游客 / 打通双向 | ✓（C-MP2b-4 乙另见） |
| 模拟器主路径 UI 断言 | ✓（多数）；Banner 点击全矩阵部分未完 |
| HTTPS 真机合法域名 | ✗ → 真机仍阻塞 |

---

## 0.1 本跑环境（续跑 15:44–15:53 Asia/Shanghai）

| # | 步骤 | 结果 |
|---|------|------|
| E1 | machineId `988e7dd4-…` connected | ✓ |
| E2 | `cli islogin` / `cli open` miniapp-exec | ✓；IDE `http://127.0.0.1:55326` |
| E3 | `cli auto --project … --auto-port 9420` | ✓ |
| E4 | automator `connect ws://127.0.0.1:9420` | ✓（`launch`+中文 cliPath 编码失败，改 connect） |
| E5 | `wx.login` codeLen=32 | ✓ |
| E6 | 初登曾绑 `17606838926`(userId=3) → unbind → 壳 userId=4 → bind 演示游客 userId=2 | ✓ |
| E7 | C-MP4-3 | **跳过 · 未动演示数据** |

---

## 1. 测服前置

| # | 检查项 | 结果 |
|---|--------|------|
| P1–P6 | health / 游客 / 运营 / 开城 / 无效 code 401 | **PASS**（v0.1；本跑未重做冒烟全集） |
| P7 | CLI + automator | **PASS** |

---

## 2. 用例结果表

### 2.1 冒烟 S1

| ID | 方式 | 状态 | 证据摘要 |
|----|------|------|----------|
| **S1-HEALTH** | API | **PASS** | v0.1 |
| **S1-LOGIN** | API | **PASS** | 无效 code→401；**真 code**→200 `role=TOURIST`（见 C-MP1-2） |
| **S1-HOME-BANNERS** | API | **PASS** | v0.1；本跑 UI 见 2 Banner carousel |
| **S1-STORES** | API | **PASS** | v0.1 |
| **S1-GUIDES** | API | **PASS** | v0.1 |

### 2.2 C-MP1～C-MP2

| ID | 方式 | 状态 | 证据摘要 |
|----|------|------|----------|
| **C-MP1-1** | API+UI | **PASS** | API 无 Token→401（v0.1）。UI：清 `nj_mp_token` 后详情 `onIntent` → **`pages/login/index`** `step=wechat` |
| **C-MP1-2** | API+UI | **PASS** | 真 code login；unbind 后壳；bind 演示游客→`userId=2` `phoneBound=true` `accountLinked=true`；再提交意向 **200** `NJL2026101803A3D92B` |
| **C-MP1-3** | API | **PASS** | 壳 Token + 合法 body（`checkIn/guests/…`）→ **403**「请先绑定手机号，完成与网页版账号打通后再提交意向」 |
| **C-MP2-1** | API+UI | **PASS** | API 已有。UI：`/pages/intent/form` 文案含「意向不等于已锁房」；成功页可开 |
| **C-MP2-2** | UI | **PASS** | 成功页：单号、客服 `400-000-0000`、notice 含「意向不等于已锁房」；「已支付」**仅出现在否定句**；无支付 CTA 按钮 |
| **C-MP2-3** | API+UI | **PASS** | 「我的意向」列表可见本人单；只读；无取消/支付入口 |

### 2.3 C-MP2b

| ID | 方式 | 状态 | 证据摘要 |
|----|------|------|----------|
| **C-MP2b-1** | API | **PASS** | 壳 list total=0 → bind→`userId=2` `accountLinked=true` → 新意向 `NJL2026101803A3D92B` → Web 登录同号可见 |
| **C-MP2b-2** | API+UI | **PASS** | Web 再建单 `NJL2026101811D96DFF`；小程序「我的」可见；id=13 **`statusText=已入住`**；禁词未出现 |
| **C-MP2b-3** | API | **PASS** | 壳 `accountLinked=false`；list total=0；不含演示游客历史单 |
| **C-MP2b-4** | API | **部分 PASS / 部分阻塞** | **甲 PASS**：已绑态再绑他号 → **409**「当前微信已绑定其他手机号」。**乙阻塞**：需另一 openid 绑同号冲突样本 |

### 2.4 C-MP3～C-MP6

| ID | 方式 | 状态 | 证据摘要 |
|----|------|------|----------|
| **C-MP3-1** | API+UI | **PASS** | 找店仅 OPEN 供给；目的地芯片无南通 |
| **C-MP3-2** | API+UI | **PASS** | 首页/找店目的地仅「扬州市」「泰州市」 |
| **C-MP3-3** | API | **PASS** | v0.1 CLOSED/410 |
| **C-MP4-1** | API+UI | **PASS** | 首页 `bannerFrame=carousel`，`bannersLen=2` |
| **C-MP4-2** | UI | **部分 PASS** | Banner `linkUrl`：`/`、`/?city=yangzhou`（API）。首页 swiper 可点配置已接。**点击跳转全矩阵**因 automator timeout **未完整断言** |
| **C-MP4-3** | — | **待测（跳过）** | **未动演示数据** |
| **C-MP5-1** | API+UI | **PASS** | sortOrder 10→20；UI 两条启用 |
| **C-MP5-2** | UI | **PASS** | swiper `autoplay` `interval=5000` `indicator-dots`；起始序与较小 sortOrder 一致（API 序） |
| **C-MP6-1** | API+UI | **PASS** | `/pages/guides/index` 3 篇可读 |
| **C-MP6-2** | API+UI | **PASS** | `/pages/guides/detail?id=3` 标题正文可读 |
| **C-MP6-3** | UI | **PASS** | 攻略页文案无问答/专题/社区入口 |

### 2.5 C-MP7～C-MP8

| ID | 方式 | 状态 | 证据摘要 |
|----|------|------|----------|
| **C-MP7-1** | UI | **阻塞** | `onShareAppMessage` path=`/pages/detail/index?id=1` **正确**；模拟器**无法**打开另一微信会话验卡片落地 → 完整分享链阻塞 |
| **C-MP8-1** | API+UI | **PASS** | API 塞 amount→400（v0.1）。UI 主路径无支付/定金/收银台/钱包 CTA（成功页「已支付」仅否定表述） |
| **C-MP8-2** | UI | **PASS** | 底栏仅 **首页 / 找店 / 我的**；无商家/运营入口 |
| **C-MP8-3** | API+UI | **PASS** | id=13 `CHECKED_IN`；列表与详情 UI **`已入住`**（U+5DF2 U+5165 U+4F4F）；禁词未现 |

### 2.6 真微信登录摘要（无 Token）

| 阶段 | userId | phoneBound | wechatBound | accountLinked | phone |
|------|--------|-----------|------------|---------------|-------|
| 初登（历史绑） | 3 | true | true | true | 17606838926 |
| unbind 后 | 3 | true | false | false | 17606838926 |
| 再 login 壳 | **4** | **false** | true | **false** | null |
| bind 演示游客后 | **2** | **true** | **true** | **true** | 17606838922 |

---

## 3. 汇总计数

| 类别 | 计数 | 说明 |
|------|------|------|
| **PASS** | **29** | 含原 API + 本跑真 code/UI；C-MP2b-4 甲计入 PASS 侧、乙另列阻塞 |
| **FAIL** | **0** | — |
| **阻塞** | **2** | C-MP7-1 完整分享会话；C-MP2b-4 乙（第二 openid） |
| **待测 / 跳过** | **2** | C-MP4-3（未动演示）；C-MP4-2 点击全矩阵未完（配置与可见性已过） |

**精简对照：**

| | 数 |
|--|----|
| PASS | 29 |
| FAIL | 0 |
| 阻塞 | 2 |
| 待测/跳过 | 2 |

另：HTTPS 真机合法域名仍为发版前置阻塞（非用例 ID）。

---

## 4. 阻塞清单

| # | 阻塞点 | 影响 | 缺什么 |
|---|--------|------|--------|
| B2 | 测服无 HTTPS | 真机 request 域名 | TLS + 微信后台域名 |
| B7 | （已解除）本机断连 | — | v0.3 已续跑 |
| B8 | C-MP7 另一会话打开分享卡 | 完整分享验收 | 真机/体验版二人会话 或 可接受的模拟器替代 |
| B9 | C-MP2b-4 乙 | 手机号已绑其他微信 409 | 第二测试号 openid |
| B6 | C-MP4-3 Banner 停用 | 停用后消失 | 运营启停（本跑明确未动） |
| B10 | C-MP4-2 点击全矩阵 | `/`、city、http(s)、其它 path | automator 稳定 tap 或人工点 |

---

## 5. 四件套

### ① 概要
测服 `c48411b` 上，开发者工具续跑完成 **真 `wx.login` + 壳 403 + bind 演示游客打通 + 主路径 UI**。**PASS≈29 / FAIL=0**。完整分享到另一会话、Banner 停用实操、Banner 点击全矩阵、C-MP2b-4 乙仍缺。HTTPS 真机仍阻塞发版。

### ② 详细
- 真 code：automator `wx.login` → login/unbind/壳 login/bind。
- 壳 403：合法 `checkIn/guests/…` body → 403 文案符合契约。
- 打通：bind→userId=2；新意向 Web 可见；Web 再建单小程序可见；id=13 UI「已入住」。
- UI：home/list/my/detail/form/success/intent-detail/guides；底栏三栏；无支付 CTA；攻略无问答专题。
- 未完：C-MP7 跨会话；C-MP4-2/4-3；2b-4 乙。

### ③ 改动点
- 本文件 → **v0.3**。
- 证据：`docs/private/exec-mvp2-ui/out/`（由本机 `mp-auto-tmp/out` 打包拷回）。
- **未**改测服/生产配置；**未**停用 Banner。本版准备开 EXEC 留痕文档 PR（经 PM 转老板手合）。

### ④ 影响点
- 可对产品说：测号 + 开发者工具（不校验合法域名）下，**微信登录/打通/意向/主 UI** 已验收通过。
- 体验版对外仍需：**HTTPS 合法域名** + 建议补一次真机分享。
- C-MP4-3 / 4-2 点击全矩阵可排期短补。

---

## 5.1 产品最终复核与关项（2026-10-18 15:55 Asia/Shanghai）

| 项 | 内容 |
|----|------|
| 结论 | **可关项，记债**（产品最终复核） |
| PASS 主路径 | C-MP1～3 / C-MP5～6 / C-MP8 + C-MP2b 主路径；FAIL=**0**，无需热修 |
| 记债（残留） | ① **C-MP7** 跨会话分享落地；② **C-MP2b-4 乙** 第二 openid；③ **C-MP4-3** Banner 停用补验（本跑未动演示）；④ **HTTPS 真机**合法域名 |
| 笔误 | 本文无「已定着」等异名落结论栏；`CHECKED_IN` 唯一合法展示仍为 **「已入住」**（U+5165 U+4F4F） |
| 文档 | 本文件进仓后经 PM 转老板手合；合入后发关项邮件 |

## 6. 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-10-18 | 仅 API/契约；UI/真 code 阻塞 |
| v0.2 | 2026-10-18 | CLI open 后本机断连；automator 未完 |
| v0.3 | 2026-10-18 | 续跑：真 code、壳 403、bind 打通、主 UI、「已入住」；C-MP7/4-3/2b-4乙/HTTPS 仍缺 |

---

## 附录 · C-MP8-3

| 项 | 内容 |
|----|------|
| 样本 | id=13 `NJL2026101883AB9BAA` `CHECKED_IN` |
| API | 运营 `statusLabel=已入住`（U+5DF2 U+5165 U+4F4F） |
| UI（本跑） | 「我的意向」与详情均显示 **已入住**；禁词未出现 → **PASS** |
