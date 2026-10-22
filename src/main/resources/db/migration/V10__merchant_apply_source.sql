-- 入驻申请来源标记：PORTAL=商家在门户自填（历史数据默认值），ADMIN=运营代录入
ALTER TABLE merchant_apply
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'PORTAL' COMMENT '来源：PORTAL=商家自填，ADMIN=运营代录' AFTER license_urls;
