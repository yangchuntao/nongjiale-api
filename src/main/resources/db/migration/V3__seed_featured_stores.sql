-- 手工精选验收店。test_data=false。不含抓取或导入房源。
-- 详情与验收以 store.package_text 为准。表弟店另有一条不同的 store_package，冲突时主文案胜。

INSERT INTO merchant_org (name, contact_name, contact_phone, status, created_at)
VALUES
    ('广陵表弟农家', '表弟', '13911110001', 'ACTIVE', CURRENT_TIMESTAMP),
    ('海陵河畔农家', '陈阿姨', '13911110002', 'ACTIVE', CURRENT_TIMESTAMP),
    ('崇川江边小院', '周师傅', '13911110003', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO tag (code, name, sort_order, enabled)
VALUES
    ('river-food', '河鲜', 10, TRUE),
    ('family', '亲子', 20, TRUE);

INSERT INTO store (
    merchant_org_id, region_id, name, address, phone, cover_url, gallery_json,
    price_text, price_from, package_text, facilities, features, notice, traffic_text,
    shelf_status, home_recommended, home_sort, test_data, version, created_at, updated_at
)
SELECT
    org.id, region.id, '表弟店', '扬州市广陵区运河东路 18 号', '13911110001', NULL, '[]',
    '¥268起', 268.00,
    '含双早与农家晚餐，可住两晚，周末不加价。',
    '独立卫浴、空调、自行车',
    '运河边、家常菜、可带小孩',
    '入住下午两点后，离店中午十二点前。',
    '公交 12 路到运河东路下。',
    'ON_SHELF', TRUE, 1, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM merchant_org org, region
WHERE org.name = '广陵表弟农家' AND region.slug = 'guangling';

INSERT INTO store (
    merchant_org_id, region_id, name, address, phone, cover_url, gallery_json,
    price_text, price_from, package_text, facilities, features, notice, traffic_text,
    shelf_status, home_recommended, home_sort, test_data, version, created_at, updated_at
)
SELECT
    org.id, region.id, '海陵河畔农家', '泰州市海陵区凤城河畔 6 号', '13911110002', NULL, '[]',
    '¥399起', 399.00,
    '临河一晚，含早餐与骑行路线。',
    '茶室、停车场',
    '早市、河边散步',
    '吸烟请到户外。',
    '城际到泰州站后打车约 15 分钟。',
    'ON_SHELF', TRUE, 2, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM merchant_org org, region
WHERE org.name = '海陵河畔农家' AND region.slug = 'hailing';

INSERT INTO store (
    merchant_org_id, region_id, name, address, phone, cover_url, gallery_json,
    price_text, price_from, package_text, facilities, features, notice, traffic_text,
    shelf_status, home_recommended, home_sort, test_data, version, created_at, updated_at
)
SELECT
    org.id, region.id, '崇川江边小院', '南通市崇川区江边巷 3 号', '13911110003', NULL, '[]',
    '¥188起', 188.00,
    '江边小院一晚，不含餐，可自带食材。',
    '厨房、院落',
    '看江、家庭入住',
    '夜间十点后请轻声。',
    '地铁 1 号线步行约 12 分钟。',
    'ON_SHELF', FALSE, 50, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM merchant_org org, region
WHERE org.name = '崇川江边小院' AND region.slug = 'chongchuan';

INSERT INTO store_package (store_id, name, price_from, description, sort_order)
SELECT id, '标间', 268.00, '仅床位，不含餐', 1
FROM store
WHERE name = '表弟店';

INSERT INTO store_tag_rel (store_id, tag_id)
SELECT store.id, tag.id
FROM store, tag
WHERE store.name = '表弟店' AND tag.code = 'river-food';

INSERT INTO store_tag_rel (store_id, tag_id)
SELECT store.id, tag.id
FROM store, tag
WHERE store.name = '崇川江边小院' AND tag.code = 'family';

INSERT INTO article (
    title, cover_url, summary, body, status, region_id, published_at, created_by, created_at, updated_at
)
SELECT
    '苏中周末住哪里',
    NULL,
    '广陵表弟店、海陵河畔和崇川小院，都是手工记下的样板。',
    '先看每家门店的套餐文案再决定。表弟店含双早与农家晚餐。',
    'PUBLISHED',
    region.id,
    CURRENT_TIMESTAMP,
    admin_user.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM region, admin_user
WHERE region.slug = 'jiangsu' AND admin_user.phone = '13800000000';
