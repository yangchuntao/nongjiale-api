# 品牌 · 站点配置项说明（已拍默认）

> 资深产品经理 · 2026-10-18  
> 老板定：站名 **垛里**｜slogan「住进千垛水乡」｜英文辅 **Duoli**；均走站点配置，随时可改；禁写死。

## 字段

| 键 | 类型 | 默认值 | 规则 |
|----|------|--------|------|
| `siteName` | string ≤64 | `垛里` | 已有；顶栏/页脚/标题/小程序导航 |
| `siteNameEn` | string ≤32 | `Duoli` | **新增**；辅文/英文；空则不展示 |
| `slogan` | string ≤40 | `住进千垛水乡` | **新增**；空则不展示副标 |
| `logoUrl` | string ≤512 URL | 见下「默认资源」 | **新增**；空→文字站名；有→顶栏/启动横版 |
| `faviconUrl` | string ≤512 URL | 见下「默认资源」 | **新增**；空→用 logo 或浏览器默认；Web favicon/方标 |
| `brandColor` | `#`+6 hex | `#4BA3D9` | 已有；禁绿作品牌色 |
| `contactPhone` | string ≤32 | `400-000-0000` | 已有 |

公开读（门户/小程序）与运营写（`GET/PUT /api/admin/site-config`）须暴露上述字段；写入后失效 `site-config` 缓存。

## 默认资源（源文件 → 静态 URL）

源文件（仓内 / 工作区）：

- 横版头图：`docs/brand/logo-final-duoli-header-横版头图.jpg`
- 方标 favicon：`docs/brand/logo-final-duoli-favicon-方标.jpg`

运维发布到测服/正式静态路径后，种子/默认配置填（示例，以运维实发为准）：

- `logoUrl` = `{静态根}/brand/logo-final-duoli-header.jpg`（或保留中文文件名，须 URL 编码）
- `faviconUrl` = `{静态根}/brand/logo-final-duoli-favicon.jpg`

未上静态前：两 URL 可先空，前端回退文字「垛里」。

## 运营怎么改

后台 **A-SITE-CONFIG**（`/admin/site`）编辑保存 → 门户与小程序读公开配置即时生效，不发版改文案。

## 验收（产品）

1. 默认展示：垛里 + 住进千垛水乡 + Duoli（有配置则示英文辅）。  
2. 改 `siteName`/`slogan`/`siteNameEn`/`logoUrl`/`faviconUrl` 后，Web 游客端/商家端/运营端相关露出 + 小程序跟配置变。  
3. 清 `logoUrl` → 回退文字站名；清 `slogan`/`siteNameEn` → 不展示对应行。  
4. 全仓无写死「站点名称」或旧占位作正式品牌文案（测试可 grep/抽检）。

## 非目标

- 本包不做支付/定金（MVP-3）。  
- 不因换 logo 改主色规则（仍浅蓝可配）。  
