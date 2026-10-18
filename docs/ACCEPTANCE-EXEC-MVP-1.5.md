# 验收结论 · EXEC-MVP-1.5 广告位（测服全量 · API + UI）

| 项 | 内容 |
|----|------|
| 日期 | **2026-10-18**（Asia/Shanghai） |
| 执行 | Grok Bot（API + UI 全量） |
| 测服 | 主用 `http://127.0.0.1:18080`；回落 `http://106.12.204.206:8080` 亦 UP |
| UI Hosts | 门户 `http://127.0.0.1:14323`；运营 `http://127.0.0.1:14324`；商家转发 `http://127.0.0.1:14325` → staging `:43125`（直连 `:43125` Chrome 失败） |
| jar / 静态 | api main **`7f4a353`（api#25）** + Flyway **V5**；静态 **`fb0f933`**（本跑冻结；**api#26 已合 main 但未热滚**） |
| 用例 | [TEST-MVP-1.5-广告位.md](./TEST-MVP-1.5-广告位.md) v0.2.1；清单 [EXEC-MVP-1.5-广告位.md](./EXEC-MVP-1.5-广告位.md) **v0.2** |
| EXEC 锁 | **已解锁**（本跑） |
| 结论规则 | **仅已执行可下 PASS/FAIL** |
| 账号 | 运营 `17606838920` / 游客 `17606838922` / 商家 `17606838921`；口令见私密备忘；**新口令已验证登录**（商家 UI 登录 OK） |

---

## 1. 摘要计数（全量）

| 结论 | 条数 |
|------|------|
| **PASS** | **18** |
| **FAIL** | **0** |
| **待测 / SKIP** | **0** |
| **阻塞** | **0** |
| **合计** | **18** |

- **无 FAIL。无待测。无阻塞。**
- **纯 UI 六条均 PASS**：C-Ad1-2、C-Ad1-3、C-Ad3-2、C-Ad5-1、C-Ad5-2、C-Ad5-3。
- 城市终态未改：扬州 **OPEN**、南通 **CLOSED**、泰州 **OPEN**。
- 测试 Banner `[EXEC-MVP15]*`（曾创建 id **3,4,5,6,7**）已全部 **DELETE**；种子 **id=1 ALL `linkUrl=/` sort 10** / **id=2 CITY 扬州 `linkUrl=/?city=yangzhou` sort 20** 保持 enabled（C-Ad1-3 测中 id=2 曾暂改外链，已还原）。

---

## 2. 登录与冒烟

| ID | 结果 | 证据 |
|----|------|------|
| S1-HEALTH | **PASS** | `GET /health` → 200 `{"service":"nongjiale-api","status":"UP"}` |
| S1-ADMIN | **PASS** | 运营登录 200/code=0，`role=OPERATOR`；新口令已验证登录 |
| S1-BANNER-ADMIN | **PASS** | API：`GET /api/admin/banners` 200。UI：可达 **`/banners`**「Banner 管理」（非 `/admin/banners`）；列表见种子；新建/编辑/删除。截图 `docs/exec-mvp15-ui/admin-banners-list.png` |
| S1-PORTAL-HOME | **PASS** | API：`GET /api/home/banners` 200。UI：登录后门户首页 Banner 区可见（匿名 `/` → 登录）；轮播 + 上/下张 + 两圆点。截图 `docs/exec-mvp15-ui/portal-home-banner.png`、`portal-carousel.png` |

游客 / 商家登录：均为 code=0 得 Token；商家 UI 登录 OK。商家另：Admin Banner **403**；无 `/api/merchant/banners`。

---

## 3. Banner API 主验收摘要

| ID | 结果 | 要点 |
|----|------|------|
| C-Ad1-1 | **PASS** | 创建 ALL 启用 Banner（例 id=3）；门户可见；UI 首页区见 S1-PORTAL-HOME |
| C-Ad1-4 | **PASS** | 带/不带 city 查询参数，ALL 集合一致 |
| C-Ad2-1 | **PASS** | disable 后门户消失 |
| C-Ad2-2 | **PASS** | 全停用后门户 `data=[]`；有启用时 FE 显示对照 S1-PORTAL-HOME |
| C-Ad3-1 | **PASS** | 门户与 Admin 列表均按 `sortOrder` **升序**；UI 起始见 C-Ad3-2 |
| C-Ad4-1 | **PASS** | CITY 扬州（id=7）入门户展示集；UI「扬州周末」入轮播见 C-Ad3-2 |
| C-Ad4-2 | **PASS**（写路径门禁） | CLOSED 南通无法写入；未做 DB 脏行过滤复测 |
| C-Ad4-3 | **PASS** | POST/PUT CITY `cityId=4` → **400**「投放市未开通，请改选全站或其他已开通市」 |

---

## 4. UI 验收摘要（本跑新增）

| ID | 结果 | 要点 |
|----|------|------|
| C-Ad1-2 | **PASS** | id=1 `href="/"`（router-link，无 target）；同 tab 停门户首页「首页 · 荷塘月色」 |
| C-Ad1-3 | **PASS** | 外链 `https://example.com` + `target="_blank" rel="noopener noreferrer"`；新开 Example Domain；原 tab 不动；测后还原 `/?city=yangzhou` |
| C-Ad3-2 | **PASS** | 两启用；圆点「第 1 / 第 2 张」；首屏 sort10「苏中逛一逛」；下一张「扬州周末」；~5s 自动（hover/focus 暂停） |
| C-Ad5-1 | **PASS** | 门户无支付/充值/押金/钱包/买广告/竞价/广告位/推广 CTA；无广告角标；图未加载 → 标题回退 |
| C-Ad5-2 | **PASS** | `/banners` 仅图片/链接/范围/排序/启用；无买广告/竞价/计费/支付/统计看板 |
| C-Ad5-3 | **PASS** | 商家 nav 仅门店+本店意向；「意向由平台客服跟进」；无广告位/Banner/买位/推广竞价 |

### 截图

| 文件 | 覆盖 |
|------|------|
| `docs/exec-mvp15-ui/portal-home-banner.png` | C-Ad1-2 / C-Ad5-1 / S1-PORTAL-HOME |
| `docs/exec-mvp15-ui/portal-carousel.png` | C-Ad3-2 / S1-PORTAL-HOME |
| `docs/exec-mvp15-ui/portal-external-newtab.png` | C-Ad1-3 |
| `docs/exec-mvp15-ui/admin-banners-list.png` | C-Ad5-2 / S1-BANNER-ADMIN |
| `docs/exec-mvp15-ui/merchant-nav.png` | C-Ad5-3 |

---

## 5. 门户终态样本（清理后）

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "title": "苏中逛一逛",
      "imageUrl": "https://example.com/njl/banner-all.png",
      "linkUrl": "/",
      "scope": "ALL",
      "sortOrder": 10,
      "enabled": true
    },
    {
      "id": 2,
      "title": "扬州周末",
      "imageUrl": "https://example.com/njl/banner-yangzhou.png",
      "linkUrl": "/?city=yangzhou",
      "scope": "CITY",
      "cityId": 2,
      "citySlug": "yangzhou",
      "cityName": "扬州市",
      "sortOrder": 20,
      "enabled": true
    }
  ]
}
```

---

## 6. Blockers / 后续

| 项 | 状态 |
|----|------|
| API 契约 / 测服滚动 | **无阻塞**（冻结 `7f4a353`+V5 / `fb0f933`；api#26 合 main **未**本跑热滚） |
| UI 验收（点击、外链新 tab、轮播、文案、商家入口页） | **已完成 · 全 PASS** |
| C-Ad4-2 历史脏行门户过滤 | 可选：运维 DB 直插 CLOSED CITY enabled 行后再验 |
| 运营路由说明 | 实测 Banner 管理为 `/banners`（`/admin/banners` 不存在）——产品/文档对齐时注意 |
| commit / PR | 文档 PR 进仓（本文件终态） |
| **产品复核** | **PASS**（2026-10-18，对照 C-Ad1～5；残留 C-Ad4-2 脏行过滤不挡关） |

---

## 7. 结论

**EXEC 全量（API + UI）：可接受 —— PASS 18 / FAIL 0 / 待测 0 / 阻塞 0。**

**产品复核 PASS（C-Ad1～5）。** 测服冻结版本下 Banner 主路径与三端发版巡检技术侧全绿；产品侧可关项。生产/热滚不在本跑范围；api#26 已合 main，测服本包不必再热滚。

---

## 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1-draft | 2026-10-18 | API 首跑计数与证据；UI 明确未做 |
| v0.2 | 2026-10-18 | 全量结论：UI 六条 + S1 UI 补全；18 PASS / 0 FAIL / 0 待测；截图路径；注明 api#26 未热滚 |
| v0.2.1 | 2026-10-18 | 产品复核 PASS（C-Ad1～5）；结论改为可关项 |
