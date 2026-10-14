-- 长三角种子 + 演示账号（密码均为 password，BCrypt）
-- password 的 BCrypt: $2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm

INSERT INTO region (slug, name, sort_order, enabled) VALUES
 ('anji', '安吉', 1, TRUE),
 ('changxing', '长兴', 2, TRUE),
 ('suzhou', '苏州', 3, TRUE),
 ('yixing', '宜兴', 4, TRUE),
 ('deqing', '德清', 5, TRUE);

INSERT INTO app_user (phone, display_name, password_hash) VALUES
 ('13800000001', '演示游客', '$2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm');

INSERT INTO admin_user (username, password_hash, display_name, role, enabled) VALUES
 ('operator', '$2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm', '运营小王', 'OPERATOR', TRUE),
 ('agent', '$2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm', '客服小李', 'AGENT', TRUE);

INSERT INTO homestay (region_id, name, address, cover_url, sales_count, status, price_from, facilities, notice, version)
SELECT id, '长兴鸿玖农家乐', '浙江省湖州市长兴县水口乡', NULL, 100, 'ON_SALE', 110.00,
       '停车场,WiFi,农家菜', '入住请提前电话确认；宠物需协商。', 0
FROM region WHERE slug = 'changxing';

INSERT INTO homestay (region_id, name, address, cover_url, sales_count, status, price_from, facilities, notice, version)
SELECT id, '宜兴翠竹昀民宿', '江苏省无锡市宜兴市西渚镇', NULL, 80, 'ON_SALE', 130.00,
       '停车场,WiFi,亲子设施', '山区信号较弱，请保持电话畅通。', 0
FROM region WHERE slug = 'yixing';

INSERT INTO homestay (region_id, name, address, cover_url, sales_count, status, price_from, facilities, notice, version)
SELECT id, '下架示例农家乐', '示例地址', NULL, 0, 'OFF_SALE', 99.00, NULL, NULL, 0
FROM region WHERE slug = 'anji';

INSERT INTO homestay_room (homestay_id, name, room_count, price_from, sort_order)
SELECT id, '标准间', 9, 110.00, 1 FROM homestay WHERE name = '长兴鸿玖农家乐';
INSERT INTO homestay_room (homestay_id, name, room_count, price_from, sort_order)
SELECT id, '大床房', 2, 120.00, 2 FROM homestay WHERE name = '长兴鸿玖农家乐';

INSERT INTO review (homestay_id, score, content, visible)
SELECT id, 5, '环境不错，适合周末放松。', TRUE FROM homestay WHERE name = '长兴鸿玖农家乐';

INSERT INTO article (title, cover_url, body, status, published_at) VALUES
 ('长兴农家乐游玩攻略', NULL, '周末去长兴，怎么选农家乐……', 'PUBLISHED', CURRENT_TIMESTAMP);

INSERT INTO home_slot (slot_type, target_id, sort_order, enabled)
SELECT 'HOT', id, 1, TRUE FROM homestay WHERE name = '长兴鸿玖农家乐';
