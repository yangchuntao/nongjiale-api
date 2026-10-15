-- 苏中种子：地区、站点占位、演示运营账号。本脚本不写门店。
-- 验收房源是手工精选（含表亲店）。抓取或导入的房源必须 test_data=true。
-- 运营登录标识是手机号，没有 username。
-- 密码明文仅本地种子：SeedAdmin#2026（BCrypt）。正式环境必须更换。
-- 码值对应 Java 枚举：AdminRole.OPERATOR、SeedMarket.SUZHONG、RegionLevel、SiteConfigKey。

INSERT INTO admin_user (phone, password_hash, display_name, role, enabled, created_at)
VALUES (
    '13800000000',
    '$2b$10$iwHoG0umd76KNgsg2CmCG.WbROH7wdTrS.gjACgMhQ5ZMazerLXQC',
    '苏中运营',
    'OPERATOR',
    TRUE,
    CURRENT_TIMESTAMP
);

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
VALUES (NULL, 'jiangsu', '江苏省', 1, 1, TRUE, 'SUZHONG');

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'yangzhou', '扬州市', 2, 10, TRUE, 'SUZHONG' FROM region WHERE slug = 'jiangsu';

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'taizhou', '泰州市', 2, 20, TRUE, 'SUZHONG' FROM region WHERE slug = 'jiangsu';

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'nantong', '南通市', 2, 30, TRUE, 'SUZHONG' FROM region WHERE slug = 'jiangsu';

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'guangling', '广陵区', 3, 11, TRUE, 'SUZHONG' FROM region WHERE slug = 'yangzhou';

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'hailing', '海陵区', 3, 21, TRUE, 'SUZHONG' FROM region WHERE slug = 'taizhou';

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'chongchuan', '崇川区', 3, 31, TRUE, 'SUZHONG' FROM region WHERE slug = 'nantong';

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
VALUES
    ('site.name', '站点名称', CURRENT_TIMESTAMP, NULL),
    ('site.brand_color', '#2F6B4F', CURRENT_TIMESTAMP, NULL),
    ('site.contact_phone', '400-000-0000', CURRENT_TIMESTAMP, NULL);
