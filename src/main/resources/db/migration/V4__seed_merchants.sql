-- 演示商家（仅开发/测试）。密码均为 password，与游客/运营同一 BCrypt。
-- 甲：13800001111 绑定「长兴鸿玖农家乐」；乙：13800002222 绑定「宜兴翠竹昀民宿」。
-- must_change_password=FALSE，便于直接调门店接口；入驻审核开通的账号仍强制改密。

INSERT INTO merchant (subject_name, enabled) VALUES
 ('演示农庄甲', TRUE),
 ('演示农庄乙', TRUE);

INSERT INTO merchant_user (merchant_id, phone, password_hash, must_change_password, enabled)
SELECT id, '13800001111', '$2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm', FALSE, TRUE
FROM merchant WHERE subject_name = '演示农庄甲';

INSERT INTO merchant_user (merchant_id, phone, password_hash, must_change_password, enabled)
SELECT id, '13800002222', '$2a$10$b5jC3oCMqndD1FrQaw01MuYTsBvD7ZYS9CA3IGl5iwG/eeLGoWTbm', FALSE, TRUE
FROM merchant WHERE subject_name = '演示农庄乙';

UPDATE homestay
SET merchant_id = (SELECT id FROM merchant WHERE subject_name = '演示农庄甲')
WHERE name = '长兴鸿玖农家乐';

UPDATE homestay
SET merchant_id = (SELECT id FROM merchant WHERE subject_name = '演示农庄乙')
WHERE name = '宜兴翠竹昀民宿';
