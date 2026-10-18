-- 首页 Banner。只收图片 URL，不建上传、支付、买位或曝光统计。
-- 不改扬州 / 泰州 / 南通的 OPEN 种子。

CREATE TABLE home_banner (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NULL,
    image_url VARCHAR(512) NOT NULL,
    link_url VARCHAR(512) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    city_id BIGINT NULL,
    sort_order INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_home_banner_city FOREIGN KEY (city_id) REFERENCES region (id),
    CONSTRAINT fk_home_banner_created_by FOREIGN KEY (created_by) REFERENCES admin_user (id),
    CONSTRAINT fk_home_banner_updated_by FOREIGN KEY (updated_by) REFERENCES admin_user (id)
);

CREATE INDEX idx_home_banner_enabled_sort ON home_banner (enabled, sort_order, id);
CREATE INDEX idx_home_banner_scope_city ON home_banner (scope, city_id);

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
WHERE admin_user.phone = '13800000000';

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
WHERE region.slug = 'yangzhou' AND region.level = 2 AND admin_user.phone = '13800000000';
