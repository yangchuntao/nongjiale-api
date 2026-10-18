-- 苏中种子：地区、站点占位、演示运营与游客。本脚本不写门店。
-- 验收房源是手工精选（含表亲店）。抓取或导入的房源必须 test_data=true。
-- 运营登录标识是手机号，没有 username。
-- 演示口令明文统一为 123456（BCrypt）。旧明文 Demo123456 与 SeedAdmin#2026 已停用。
-- 正式环境必须更换。本脚本只给全新库；不另写迁移去改已有库的口令或手机号。
-- 码值对应 Java 枚举：AdminRole.OPERATOR、SeedMarket.SUZHONG、RegionLevel、SiteConfigKey。

INSERT INTO admin_user (phone, password_hash, display_name, role, enabled, created_at)
VALUES (
    '17606838920',
    '$2a$10$f50zaGUPPjk7wkAofwZry.O0EsZAH65NgQ6eQ6pTDNHl9NjvuoROG',
    '苏中运营',
    'OPERATOR',
    TRUE,
    CURRENT_TIMESTAMP
);

INSERT INTO portal_user (phone, password_hash, display_name, created_at)
VALUES (
    '17606838922',
    '$2a$10$f50zaGUPPjk7wkAofwZry.O0EsZAH65NgQ6eQ6pTDNHl9NjvuoROG',
    '演示游客',
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
    ('site.brand_color', '#4BA3D9', CURRENT_TIMESTAMP, NULL),
    ('site.contact_phone', '400-000-0000', CURRENT_TIMESTAMP, NULL);
