-- 全国行政区划支持：region 增加行政区划代码、数据来源、短名与驻地经纬度。
--
-- adcode 是国家标准行政区划代码（如 320500 苏州市），全局唯一，作为区划同步的幂等键。
-- 存量种子没有 adcode，这里按 slug 反填江苏段，其余保持 NULL（NULL 不参与唯一约束）。
--
-- seed_market 原本 NOT NULL 且只允许 SUZHONG，全国数据不属于任何种子市场，改为可空。
--
-- source 区分数据来源：SEED 迁移种子、SYNC 区划同步导入；便于排查和后续增量更新。

ALTER TABLE region ADD COLUMN adcode VARCHAR(12) NULL;
ALTER TABLE region ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'SEED';
ALTER TABLE region ADD COLUMN short_name VARCHAR(32) NULL;
ALTER TABLE region ADD COLUMN center_lng DECIMAL(10, 6) NULL;
ALTER TABLE region ADD COLUMN center_lat DECIMAL(10, 6) NULL;

-- 存量种子补 adcode，保证与全国同步数据能按 adcode 对上，不会重复插入
UPDATE region SET adcode = '320000' WHERE slug = 'jiangsu';
UPDATE region SET adcode = '321000' WHERE slug = 'yangzhou';
UPDATE region SET adcode = '321200' WHERE slug = 'taizhou';
UPDATE region SET adcode = '320600' WHERE slug = 'nantong';
UPDATE region SET adcode = '321002' WHERE slug = 'guangling';
UPDATE region SET adcode = '321202' WHERE slug = 'hailing';
UPDATE region SET adcode = '320602' WHERE slug = 'chongchuan';
-- V8 苏州种子：姑苏区在国家标准里是 320508，其余按标准代码补齐
UPDATE region SET adcode = '320500' WHERE slug = 'suzhou';
UPDATE region SET adcode = '320505' WHERE slug = 'huqiu';
UPDATE region SET adcode = '320506' WHERE slug = 'wuzhong';
UPDATE region SET adcode = '320507' WHERE slug = 'xiangcheng';
UPDATE region SET adcode = '320508' WHERE slug = 'gusu';
UPDATE region SET adcode = '320509' WHERE slug = 'wujiang';

-- 唯一约束：同一行政区划代码只能有一行；NULL 不冲突，存量为空的行不受影响
ALTER TABLE region ADD CONSTRAINT uk_region_adcode UNIQUE (adcode);

-- 全国数据量约 3200 行，按父级查子级是最频繁的操作
CREATE INDEX idx_region_adcode ON region (adcode);

-- seed_market 放开为可空：全国区划不属于任何种子市场
ALTER TABLE region MODIFY COLUMN seed_market VARCHAR(32) NULL;
