# 品牌站点配置 · 技术设计

> 设计稿。待产品门禁 + 老板 TECH 终验后再写业务代码。  
> 本文件只定契约。不改 Java、Flyway、控制器或测试。  
> 产品稿：`docs/PRODUCT-品牌-站点配置.md`（[api#36](https://cursor.com/codebase/andrew-yang/nongjiale-api/pull/36)，已拍默认）。本 TECH 与该稿对齐，并写入运维已挂上的测服静态地址。  
> 运营字段表：`docs/ADMIN-API.md`。门户入口：`README.md`、`BE4-DELIVERY.md`。  
> 运营页面仍是 **A-SITE-CONFIG**，路径 `/admin/site`（前端仓）。本仓不新增页面，也不新开资源。

已锁参数：公开读 + 运营写。扩既有 `GET/PUT /api/admin/site-config` 与 `GET /api/config/public`。站名、英文辅文、副标、Logo、图标都进 `site_config`，用 `SiteConfigKey` 的码，不在业务代码里写魔法字符串。默认文案和默认图只出现在 Flyway 种子里。Java 与前端都不写死「垛里」「Duoli」「住进千垛水乡」或测服图片地址。`brandColor` / `contactPhone` 的规则与种子不变。开城门槛 `open_city.min_stores`、`open_city.min_articles` 仍不出现在这两个接口里。`CHECKED_IN` 文案仍是「已入住」。

写成功后失效缓存名 `site-config`、键 `public`（Redis 键 `njl:site-config:public`）。Logo / 图标由运维做静态托管，后端只存字符串。本包不做上传接口。

## ①概要设计

`site_config` 继续是一行一个键。表结构不改：`config_key VARCHAR(64)` 主键，`config_value VARCHAR(2000) NOT NULL`。品牌字段都是字符串，空串合法，不存 NULL。

在已有三键旁边增加四键：

| JSON 字段 | `SiteConfigKey` | 库内码 | 本包 |
|-----------|-----------------|--------|------|
| `siteName` | `NAME` | `site.name` | 已有。种子占位「站点名称」改为「垛里」 |
| `siteNameEn` | `NAME_EN` | `site.name_en` | 新增。英文辅文 |
| `slogan` | `SLOGAN` | `site.slogan` | 新增。副标 |
| `logoUrl` | `LOGO_URL` | `site.logo_url` | 新增。横版头图 URL |
| `faviconUrl` | `FAVICON_URL` | `site.favicon_url` | 新增。方标 / favicon URL |
| `brandColor` | `BRAND_COLOR` | `site.brand_color` | 已有。不改规则 |
| `contactPhone` | `CONTACT_PHONE` | `site.contact_phone` | 已有。不改规则 |

`OPEN_CITY_MIN_STORES`（`open_city.min_stores`）和 `OPEN_CITY_MIN_ARTICLES`（`open_city.min_articles`）留在枚举里，继续只被开城门槛读取。`GET/PUT /api/admin/site-config` 不返回、不写入这两键。

两条接口的 `data` 使用同一组七个字段，名字相同。门户、商家端露出、运营端露出、小程序都读 `GET /api/config/public`，不另开「品牌」资源，也不按端拆字段。

运营保存仍是全量 PUT。`siteName`、`brandColor`、`contactPhone` 保持必填。四个新字段允许空串；省略或 JSON `null` 与空串一样，存成空串。空串的展示回退由前端做，接口不把空的 `faviconUrl` 填成 `logoUrl`，也不把空的站名填成「垛里」。

下一版迁移是 **V8**（现网最新脚本是 `V7__repair_home_banner_seed.sql`）。V8 只做数据：改占位站名，插入四个新键。不改 `V2__seed_suzhong.sql`（已入库脚本的校验和不动）。

## ②详细设计

### 现状（按代码，实现时沿用）

| 事实 | 落点 |
|------|------|
| 技术栈 | Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、Redis 只做缓存、Bearer。枚举走码，不写魔法字符串 |
| 表 | `site_config`。实体 `SiteConfig`，主键 `config_key`，`IdType.INPUT` |
| 现有品牌键 | `SiteConfigKey.NAME` / `BRAND_COLOR` / `CONTACT_PHONE`。V2 种子：站名「站点名称」、主色 `#4BA3D9`、电话 `400-000-0000` |
| 开城键 | 同枚举另两码。`OpenCityThresholds` 直接读库，缺行、空串、非正整数回退 5 和 1。不进站点配置接口 |
| 运营读写 | `AdminSiteConfigController`：`GET/PUT /api/admin/site-config`。`AdminSiteConfigService` 按键 upsert，审计 `AuditAction.SITE_CONFIG_UPDATE`、`AuditTarget.SITE_CONFIG`，然后 `AdminCache.evictPublicConfig()` |
| 公开读 | `PublicConfigController`：`GET /api/config/public`，无登录。`PublicConfigService` 走 `CacheAside`，缓存 `CacheName.SITE_CONFIG`，键 `public` |
| Redis 键 | 前缀 `app.cache.key-prefix`（默认 `njl:`）+ `site-config` + `:` + `public`，即 `njl:site-config:public`。TTL `app.cache.site-config-ttl`（30 分钟） |
| 运营鉴权 | `/api/admin/**`：`OPERATOR` 或 `AGENT` 的 Bearer。未登录 401，游客或商家令牌 403。本包不按角色再拆 |
| 公开鉴权 | `GET /api/config/public` 落在 `anyRequest().permitAll()`。本包不加匹配规则 |
| 未知 JSON 字段 | `fail-on-unknown-properties: true`，400「请求体格式不正确」 |
| 校验文案 | Bean Validation 失败时取第一条字段错误。现有三字段的句子已经写在 `SiteConfigWriteRequest` 上 |
| 缺行 | `value()`：行不存在或 `config_value` 为 null 时返回空串。本包保持这个行为，不在 Java 里补默认文案 |
| 成功信封 | `{code:0, message:"ok", data}`。失败 `code` 等于 HTTP 状态，`data` 为 null |
| 上传 | 全库没有 `MultipartFile`、没有对象存储、没有上传控制器。广告位 TECH 已选定图片只收 URL（`docs/TECH-DESIGN-MVP-1.5-广告位.md`「图片」；`BannerRules` 只校验 `http://` / `https://` 前缀，最长 512） |
| 时钟 | `ClockConfig.ZONE = Asia/Shanghai` |
| 意向 | `IntentStatus.CHECKED_IN` 标签「已入住」。本包不改这个枚举，也不改状态迁移 |

### 字段

长度按 Java `String.length()`（UTF-16 代码单元），与现有 `@Size` 一致。「住进千垛水乡」是 6，「Duoli」是 5。

| JSON | 类型 | 种子 / 测服默认 | 写入规则 | 空值时前端 |
|------|------|-----------------|----------|------------|
| `siteName` | string ≤64 | `垛里` | 已有。`@NotBlank`「站名不能为空」，`@Size(max=64)`「站名过长」。服务里 `trim` 后入库 | 接口不允许空 |
| `siteNameEn` | string ≤32 | `Duoli` | **新增**。trim 后可空。超过 32：400「英文站名过长」 | 不展示英文辅文 |
| `slogan` | string ≤40 | `住进千垛水乡` | **新增**。trim 后可空。超过 40：400「副标过长」 | 不展示副标 |
| `logoUrl` | string ≤512 | 测服 `http://106.12.204.206:43123/brand/logo-header.jpg` | **新增**。空串允许。非空须 `http://` 或 `https://` | 文字站名（用当时的 `siteName`） |
| `faviconUrl` | string ≤512 | 测服 `http://106.12.204.206:43123/brand/logo-favicon.jpg` | **新增**。规则同 `logoUrl` | 有 `logoUrl` 则用 logo，否则浏览器默认图标 |
| `brandColor` | string | `#4BA3D9` | 已有。必填，`^#[0-9A-Fa-f]{6}$`，「主色格式不正确」。禁绿不另做色相校验，仍只验这 6 位 | 沿用现网 |
| `contactPhone` | string ≤32 | `400-000-0000` | 已有。必填，「客服电话不能为空」/「客服电话过长」 | 沿用现网 |

`siteNameEn` 的上限是 **32**，与产品稿 api#36 一致。站名仍是 64，两者不共用一个上限。

GET 的字段顺序固定为：`siteName`、`siteNameEn`、`slogan`、`logoUrl`、`faviconUrl`、`brandColor`、`contactPhone`。值为字符串。缺行、库内空串、以及缓存里旧 JSON 缺字段，响应里都是 `""`，不是 `null`。

### `SiteConfigKey` 枚举

实现时只加四个常量。已有常量的码不变。`@EnumValue` 仍打在 `code` 上，MyBatis 读写都用码。

| 常量 | `code()` | 谁读 | 本包 |
|------|----------|------|------|
| `NAME` | `site.name` | 站点配置接口、公开配置 | 种子改占位 |
| `NAME_EN` | `site.name_en` | 同上 | 新增 |
| `SLOGAN` | `site.slogan` | 同上 | 新增 |
| `LOGO_URL` | `site.logo_url` | 同上 | 新增 |
| `FAVICON_URL` | `site.favicon_url` | 同上 | 新增 |
| `BRAND_COLOR` | `site.brand_color` | 同上 | 不改 |
| `CONTACT_PHONE` | `site.contact_phone` | 同上 | 不改 |
| `OPEN_CITY_MIN_STORES` | `open_city.min_stores` | 仅 `OpenCityThresholds` | 不进站点配置接口 |
| `OPEN_CITY_MIN_ARTICLES` | `open_city.min_articles` | 仅 `OpenCityThresholds` | 不进站点配置接口 |

服务、DTO、测试断言键名时引用 `SiteConfigKey.NAME_EN.code()` 这类常量。不要出现 `"site.name_en"` 这类字面量。审计 `detail` 的 JSON 键用接口字段名（`siteNameEn`），与现网 `siteName` / `brandColor` / `contactPhone` 一样，那些键不是库内码。

新增文案放进枚举 `SiteConfigMessage`（状态 + 中文句子）。校验只抛这个枚举，不在服务方法里另写中文。已有三字段继续用 `SiteConfigWriteRequest` 上的 Jakarta 句子，不搬进这个枚举，避免同一句话两处维护。

| 常量 | HTTP | 句子 |
|------|------|------|
| `NAME_EN_TOO_LONG` | 400 | 英文站名过长 |
| `SLOGAN_TOO_LONG` | 400 | 副标过长 |
| `LOGO_TOO_LONG` | 400 | Logo 地址过长 |
| `LOGO_SCHEME` | 400 | Logo 地址须为 http 或 https |
| `FAVICON_TOO_LONG` | 400 | 图标地址过长 |
| `FAVICON_SCHEME` | 400 | 图标地址须为 http 或 https |

### 种子与测服默认（V8 草案）

本 PR 不提交 SQL。实现 PR 新增 `src/main/resources/db/migration/V8__brand_site_config.sql`，内容以下面为准。H2 `MODE=MySQL` 与 MySQL 8 都用 `INSERT … SELECT … WHERE NOT EXISTS`，与 V7 的写法相同。不建新表，不加列。

测服静态已由运维挂好。种子里的 Logo / 图标就写这两条绝对地址，不再留空等「以后再填」：

| 键 | 种子值 | 说明 |
|----|--------|------|
| `site.name` | `垛里` | 仅当当前值仍是占位「站点名称」时更新。运营已经改过的站名不覆盖 |
| `site.name_en` | `Duoli` | 键不存在才插入。已有行不覆盖 |
| `site.slogan` | `住进千垛水乡` | 同上 |
| `site.logo_url` | `http://106.12.204.206:43123/brand/logo-header.jpg` | 测服横版头图。同上 |
| `site.favicon_url` | `http://106.12.204.206:43123/brand/logo-favicon.jpg` | 测服方标。同上 |
| `site.brand_color` | （不写） | 保持 `#4BA3D9` |
| `site.contact_phone` | （不写） | 保持 `400-000-0000` |

产品稿 api#36 里的源文件是设计资产，不是 URL：

- 横版头图：`docs/brand/logo-final-duoli-header-横版头图.jpg`
- 方标：`docs/brand/logo-final-duoli-favicon-方标.jpg`

运维实发的路径是 `/brand/logo-header.jpg` 与 `/brand/logo-favicon.jpg`。种子以实发 URL 为准。生产环境用同一条 V8 时，首次迁移也会写入上述测服地址；上线后由运营在 `/admin/site` 改成生产静态根。本包不按 profile 分叉 Flyway，也不在 Java 里按环境替换 URL。生产把某个 URL 存成空串时，前端按上表回退，后端不补测服地址。

```sql
-- V8__brand_site_config.sql
-- 草案。实现 PR 才落盘。不改表结构。

UPDATE site_config
SET config_value = '垛里',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'site.name'
  AND config_value = '站点名称';

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
SELECT 'site.name_en', 'Duoli', CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM site_config WHERE config_key = 'site.name_en');

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
SELECT 'site.slogan', '住进千垛水乡', CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM site_config WHERE config_key = 'site.slogan');

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
SELECT 'site.logo_url', 'http://106.12.204.206:43123/brand/logo-header.jpg', CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM site_config WHERE config_key = 'site.logo_url');

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
SELECT 'site.favicon_url', 'http://106.12.204.206:43123/brand/logo-favicon.jpg', CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM site_config WHERE config_key = 'site.favicon_url');
```

`updated_by` 在种子里为 NULL，与 V2 的三行一致。运营之后的 PUT 仍写入当前管理员 id。

### 校验

PUT 先走现有 Jakarta 校验（站名、主色、电话），再走 `SiteConfigRules`。新字段先 `trim`，再量长度、再看协议。只含空白的新字段当成空串入库。

URL 与 `BannerRules` 的图片地址同一形态，但空串合法，且句子用 `SiteConfigMessage`，不复用「请填写图片地址」「图片地址过长」（那些句子表示必填的 Banner 图）。

非空 URL 同时满足：

1. 长度 ≤ 512。
2. 以 `http://` 或 `https://` 开头。协议按小写比较，`HTTP://` 拒绝。与 Banner 图片地址相同。
3. trim 之后不再含空格、换行、制表。含空白时用对应的「须为 http 或 https」。
4. 不接受站内相对路径、`//host`、`javascript:`、`data:`。

不解析主机，不发 HEAD，不要求以 `.jpg` 结尾。测服文件是 `.jpg`；运维换成 `.png` 或带查询串，只要仍是 http(s) 且不超过 512，就可以存。地址能存不代表文件仍在；裂图由前端占位。

命中顺序（新字段之间）：`siteNameEn` 过长 → `slogan` 过长 → Logo 过长 → Logo 协议 → 图标过长 → 图标协议。前面的 Jakarta 错误先返回。未知字段、坏 JSON 仍是「请求体格式不正确」，不进 `SiteConfigMessage`。

```java
static String optionalText(String raw, int max, SiteConfigMessage tooLong) {
    if (raw == null) {
        return "";
    }
    String value = raw.trim();
    if (value.isEmpty()) {
        return "";
    }
    if (value.length() > max) {
        throw tooLong.toException();
    }
    return value;
}

static String optionalHttpUrl(String raw, SiteConfigMessage tooLong, SiteConfigMessage scheme) {
    String value = optionalText(raw, 512, tooLong);
    if (value.isEmpty()) {
        return "";
    }
    boolean http = value.startsWith("http://") || value.startsWith("https://");
    if (!http || containsWhitespace(value)) {
        throw scheme.toException();
    }
    return value;
}
```

### Logo / 图标：URL 手工填

仓内没有可复用的通用上传。门店封面、攻略封面、首页 Banner 都是调用方自己填的 URL。广告位实现（`BannerRules.requiredHttpUrl`）拒绝空图；品牌图允许空，所以新写 `optionalHttpUrl`，不把 Banner 的必填方法硬套过来。

本包选定：**后端只存字符串，不新增上传接口，不接对象存储。**

- 运维把文件放到静态站点，把绝对 URL 交给运营。
- 运营在 A-SITE-CONFIG（`/admin/site`）把 URL 贴进 `logoUrl` / `faviconUrl`。失焦预览由前端做。
- 测服默认已经是上面的两条 `http://106.12.204.206:43123/brand/...`。生产改 URL 走 PUT，不改代码。
- 前端不要调用不存在的上传路径。

以后若要做上传，另开包。`site.logo_url` 与 `site.favicon_url` 的含义仍是「一条 http(s) 字符串」。

### 读写、审计、缓存

DTO 扩成七个字段。`SiteConfigResponse` 与 `PublicConfigResponse` 字段相同，公开响应不省略空字段。

`AdminSiteConfigService.get` / `PublicConfigService.load` 按枚举顺序读七个键。`update` 对七个键各 upsert 一次：已有三字段仍 `trim` 后写入；四个新字段写入 `SiteConfigRules` 的结果（可能是空串）。缺行则 insert，已有行则 update，与现有 `upsert` 相同。

审计仍是一条 `SITE_CONFIG_UPDATE`，目标 `SITE_CONFIG`，`target_id` 仍为 null。`detail` 增加 `siteNameEn`、`slogan`、`logoUrl`、`faviconUrl`，值为入库后的字符串。不新增 `AuditAction`。

成功路径的最后一步仍是 `adminCache.evictPublicConfig()`，只删 `njl:site-config:public`。不删首页、地区、门店详情。删缓存失败只记日志，不回滚数据库（`AdminCache` 现有约定）。PUT 的响应用删缓存之后的 `get()`，直接读库，不读这把缓存。

公开读继续 cache-aside。Redis 失败或超时则读库，不返回 5xx。

发版时 Redis 里可能还是旧的三字段 JSON。Jackson 对 record 缺字段会得到 null，而契约要求空串；更要紧的是，命中旧缓存时不会读到 V8 刚插入的 Logo。实现 PR 在应用就绪后失效一次 `CacheName.SITE_CONFIG` / `public`（失败只记日志）。读路径仍把 null 收成 `""`，避免漏失效时门户拿到 null。运营 GET 不走这把缓存，迁移一完成就能看见种子。

### API 契约差分

路径、方法、鉴权、信封都不新增。差分只在 `data`。

**现网（实现合入前）**

`GET /api/config/public` 与 `GET/PUT /api/admin/site-config` 的 `data`：

```json
{
  "siteName": "站点名称",
  "brandColor": "#4BA3D9",
  "contactPhone": "400-000-0000"
}
```

PUT 只接受这三个字段。多传 `siteNameEn` 等会 400「请求体格式不正确」。因此前端要等实现 PR 合入后再提交新字段。

**目标（实现合入后）**

`GET /api/config/public` 与 `GET/PUT /api/admin/site-config` 的 `data`：

```json
{
  "siteName": "垛里",
  "siteNameEn": "Duoli",
  "slogan": "住进千垛水乡",
  "logoUrl": "http://106.12.204.206:43123/brand/logo-header.jpg",
  "faviconUrl": "http://106.12.204.206:43123/brand/logo-favicon.jpg",
  "brandColor": "#4BA3D9",
  "contactPhone": "400-000-0000"
}
```

上表是全新库跑完 V8、且占位站名仍是「站点名称」时的默认。生产改过的值以库为准。

PUT 请求体：

| 字段 | 必填 | 空串 |
|------|------|------|
| `siteName` | 是 | 400「站名不能为空」 |
| `siteNameEn` | 可省略 | 存 `""`，公开读回 `""` |
| `slogan` | 可省略 | 存 `""` |
| `logoUrl` | 可省略 | 存 `""` |
| `faviconUrl` | 可省略 | 存 `""` |
| `brandColor` | 是 | 400「主色不能为空」；格式仍是 6 位十六进制 |
| `contactPhone` | 是 | 400「客服电话不能为空」 |

省略新字段等于清空该键，不是「保留旧值」。这是全量 PUT，不是补丁。实现合入后，仍只提交三字段的旧运营页每次保存都会把四个新键写成空串，测服默认图会被清掉。运营页 `/admin/site` 与本接口同一窗口发布。

清空示例（只表达空串语义，其余字段照常带上）：

```json
{
  "siteName": "垛里",
  "siteNameEn": "",
  "slogan": "",
  "logoUrl": "",
  "faviconUrl": "",
  "brandColor": "#4BA3D9",
  "contactPhone": "400-000-0000"
}
```

此时公开读回同样的空串。前端不展示英文辅文和副标；顶栏用文字「垛里」；图标走浏览器默认（因为 logo 也空了）。

### 验收要点

实现 PR 用这些要点对契约，本设计 PR 不跑测试。

1. 全新库跑完 V8：`site.name` 为「垛里」，`site.name_en` 为「Duoli」，`site.slogan` 为「住进千垛水乡」，`site.logo_url` 与 `site.favicon_url` 为测服两条 URL。主色仍是 `#4BA3D9`，电话仍是 `400-000-0000`。开城两键不变。
2. 库内 `site.name` 已不是「站点名称」时，V8 不改它。四个新键若已存在，V8 不覆盖。
3. `GET /api/config/public` 无登录返回七字段。`GET /api/admin/site-config` 在运营 Bearer 下返回同一组值。游客或商家令牌调运营接口仍是 403，未登录 401。
4. PUT 合法七字段后，再读公开配置即见新值（缓存已失效，不必等 30 分钟）。审计一条 `SITE_CONFIG_UPDATE`，`detail` 含七个字段。
5. `slogan` 第 41 个字符 400「副标过长」。`siteNameEn` 第 33 个字符 400「英文站名过长」。`logoUrl` / `faviconUrl` 空串成功；`https://example.com/a.png` 成功；`/brand/logo.jpg`、`javascript:alert(1)`、带空格的地址 400，句子区分 Logo 与图标。超过 512 为「Logo 地址过长」或「图标地址过长」。
6. 站名空、主色不是 `#` 加 6 位、电话空，文案与现网相同。未知字段 400「请求体格式不正确」。
7. 公开响应与运营 GET 在空串时返回 `""`，不回填「垛里」、测服 URL 或把空 favicon 换成 logo。
8. Java 源码与种子以外的资源不出现「垛里」「Duoli」「住进千垛水乡」以及 `106.12.204.206`。测试断言种子值时引用枚举码读取数据库，不把品牌句子散落在业务分支里。
9. 不新增上传路径。`POST /api/admin/uploads` 这类未实现路径仍是 404「接口不存在」。
10. `CHECKED_IN` 的中文仍是「已入住」。意向接口无改动。

### 不做

支付、定金、收银台、`/api/payments`。上传接口与对象存储。按环境写死的品牌文案或图片地址。新的站点配置路径、新的公开「品牌」资源。把开城 N/M 放进 `GET/PUT /api/admin/site-config`。改主色校验、改客服电话上限、把品牌色改成绿色。改 `CHECKED_IN` 的码或「已入住」文案。在接口里把空 `faviconUrl` 替换成 `logoUrl`（回退只在前端）。本 PR 不改 `src/` 与 `db/migration`。

## ③改动点

**本 PR（设计稿）**

- 新增 `docs/TECH-DESIGN-品牌站点配置.md`（本文）。
- `docs/ADMIN-API.md` 的「站点配置」改为七字段目标契约，并写明实现合入前现网仍是三字段。
- `README.md`：文档索引、门户公开配置、运营 `site-config` 一行，指向本文。
- `BE4-DELIVERY.md`：公开配置一行注明品牌字段的目标契约在本文；该交付记录本身仍是当时的三字段。
- 不改 `src/`、`src/main/resources/db/migration`、测试、`pom.xml`。
- 不改产品稿 api#36 的文件（该 PR 自带设计资产）。不改 `CHECKED_IN` 相关文档。

**终验通过后的实现（本 PR 不做）**

- `SiteConfigKey` 增加 `NAME_EN`、`SLOGAN`、`LOGO_URL`、`FAVICON_URL`。新增 `SiteConfigMessage`、`SiteConfigRules`。
- `SiteConfigWriteRequest`、`SiteConfigResponse`、`PublicConfigResponse` 增加四个字段。
- `AdminSiteConfigService` 读写七键；审计 `detail` 带上新字段；写后仍调用 `evictPublicConfig()`。
- `PublicConfigService.load` 读七键。应用就绪后失效一次 `site-config` / `public`。读出的 null 收成空串。
- V8 脚本按上文草案入库。不改 V2。
- 现网断言「站点名称」的测试改为断言 V8 之后的种子：`AdminApiIntegrationTest`、`AdminAuthIntegrationTest`、`PortalApiIntegrationTest`。补上空 URL、超长副标、非法协议、缓存失效。
- 实现阶段若契约有修订，再改 `docs/ADMIN-API.md` 与 README，使它们与代码一致。

## ④影响点

- 门户与小程序：实现之后，`GET /api/config/public` 多四个字段。旧客户端按名读取原三字段仍然可用。展示英文辅文、副标、Logo、图标要等前端改读新字段。空串的回退规则见字段表，后端不代填。
- 测服：V8 之后、且缓存已失效时，公开配置直接带上 `http://106.12.204.206:43123/brand/logo-header.jpg` 与 `logo-favicon.jpg`。这两条是 http，不是 https；校验允许 http。生产首次跑同一条迁移也会得到测服地址，须在 `/admin/site` 改成生产 URL，或存空串走文字站名 / 默认图标。
- 运营：页面仍是 `/admin/site`（A-SITE-CONFIG）。表单一次提交七个字段。只发后端、不发运营页时，旧页面保存会清空四个新键。`OPERATOR` 与 `AGENT` 都能写，与现网相同。
- 缓存：键仍是 `njl:site-config:public`，TTL 仍是 30 分钟。忘记失效时，门户最长 30 分钟看不到 V8 的 Logo。运营 GET 不走缓存。首页、地区、门店详情的键不动。
- 开城：N/M 的读取与缺省回退不变。本包的 PUT 不会改 `open_city.min_stores` 与 `open_city.min_articles`。
- 商家：无新接口。商家令牌访问 `/api/admin/site-config` 仍是 403。商家端若要显示站名或 Logo，读公开配置。
- 意向：状态机与「已入住」不变。全站仍无支付路径。
- 图片：没有上传 API。静态文件由运维提供，接口只保存 URL 字符串。
- 历史文档：MVP-1 / MVP-1.5 / MVP-2 里「站点配置仍是三项」「公开配置契约零改动」描述的是那些包自己的范围。品牌包改的是同一条路径上的字段集合。那些已终验的文档本 PR 不改写。
- 覆盖率：实现阶段新代码纳入现有 JaCoCo。`mvn test` 的 LINE / COVEREDRATIO 门禁仍是 **≥ 0.85**（`jacoco.line.minimum`，见 `docs/COVERAGE.md`）。本 PR 无 Java 改动，不跑 `mvn test`。
- 依赖：依赖现有 `site_config`、运营 Bearer、`CacheAside` 与 `SITE_CONFIG_UPDATE` 审计。不依赖支付，也不改微信登录。
