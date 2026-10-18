-- V5 仍按旧演示手机号 13800000000 查找运营。口令与手机号更换后那次插入匹配不到行。
-- 这里按当前演示运营 17606838920 补上同一对种子。已有同名标题时不重复插入。

INSERT INTO home_banner (
    title, image_url, link_url, scope, city_id, sort_order, enabled, created_by, updated_by, created_at, updated_at
)
SELECT
    '苏中逛一逛',
    'https://example.com/njl/banner-all.png',
    '/',
    'ALL',
    NULL,
    10,
    TRUE,
    admin_user.id,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM admin_user
WHERE admin_user.phone = '17606838920'
  AND NOT EXISTS (SELECT 1 FROM home_banner WHERE title = '苏中逛一逛');

INSERT INTO home_banner (
    title, image_url, link_url, scope, city_id, sort_order, enabled, created_by, updated_by, created_at, updated_at
)
SELECT
    '扬州周末',
    'https://example.com/njl/banner-yangzhou.png',
    '/?city=yangzhou',
    'CITY',
    region.id,
    20,
    TRUE,
    admin_user.id,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM region, admin_user
WHERE region.slug = 'yangzhou'
  AND region.level = 2
  AND admin_user.phone = '17606838920'
  AND NOT EXISTS (SELECT 1 FROM home_banner WHERE title = '扬州周末');
