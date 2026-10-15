-- MVP-0 核心表。H2 MODE=MySQL 与 MySQL 8 共用。
-- 枚举列统一 VARCHAR(32)，码值与 Java 枚举 code 一致，只增不改名。
-- 大文本用 TEXT（两边都能跑；不使用仅 H2 的 CLOB）。
-- 一期 booking_intent 不含支付列。同一门店仅一条 PENDING 上下架申请由应用层保证（两边都不做部分唯一索引）。
-- 门店详情与验收以 store.package_text 为准。store_package 可以没有行；两边不一致时 package_text 赢。
-- 验收用的精选店是手工整理（含表亲店），不是抓取来的。抓取或导入的房源必须 test_data=true。

CREATE TABLE portal_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_portal_user_phone UNIQUE (phone)
);

CREATE TABLE admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_user_phone UNIQUE (phone)
);

CREATE TABLE merchant_org (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NULL,
    contact_phone VARCHAR(20) NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE merchant_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_org_id BIGINT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_user_phone UNIQUE (phone),
    CONSTRAINT fk_merchant_user_org FOREIGN KEY (merchant_org_id) REFERENCES merchant_org (id)
);

CREATE INDEX idx_merchant_user_org ON merchant_user (merchant_org_id);

CREATE TABLE region (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    slug VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    level TINYINT NOT NULL,
    sort_order INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    seed_market VARCHAR(32) NOT NULL,
    CONSTRAINT uk_region_slug UNIQUE (slug),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES region (id)
);

CREATE INDEX idx_region_parent ON region (parent_id);
CREATE INDEX idx_region_seed_enabled ON region (seed_market, enabled);

CREATE TABLE merchant_apply (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NULL,
    contact_phone VARCHAR(20) NULL,
    planned_store_name VARCHAR(128) NULL,
    region_id BIGINT NULL,
    description VARCHAR(1000) NULL,
    license_urls TEXT NULL,
    status VARCHAR(32) NOT NULL,
    reject_reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    merchant_org_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_apply_region FOREIGN KEY (region_id) REFERENCES region (id),
    CONSTRAINT fk_merchant_apply_reviewer FOREIGN KEY (reviewed_by) REFERENCES admin_user (id),
    CONSTRAINT fk_merchant_apply_org FOREIGN KEY (merchant_org_id) REFERENCES merchant_org (id)
);

CREATE INDEX idx_merchant_apply_status ON merchant_apply (status, created_at);

CREATE TABLE store (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_org_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(256) NULL,
    phone VARCHAR(32) NULL,
    cover_url VARCHAR(512) NULL,
    gallery_json TEXT NULL,
    price_text VARCHAR(64) NULL,
    price_from DECIMAL(10, 2) NULL,
    -- 套餐权威文案。详情和验收只认这一列；store_package 可空，冲突时本列优先。
    package_text TEXT NULL,
    facilities TEXT NULL,
    features TEXT NULL,
    notice TEXT NULL,
    traffic_text TEXT NULL,
    shelf_status VARCHAR(32) NOT NULL,
    home_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    home_sort INT NOT NULL DEFAULT 100,
    -- 验收精选店为手工数据，保持 false。抓取或导入的房源必须为 true。
    test_data BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_org FOREIGN KEY (merchant_org_id) REFERENCES merchant_org (id),
    CONSTRAINT fk_store_region FOREIGN KEY (region_id) REFERENCES region (id)
);

CREATE INDEX idx_store_shelf_region ON store (shelf_status, region_id);
CREATE INDEX idx_store_home ON store (home_recommended, home_sort);
CREATE INDEX idx_store_org ON store (merchant_org_id);

-- 套餐明细可选。没有行是正常情况，不能拿它覆盖 store.package_text。
CREATE TABLE store_package (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    price_from DECIMAL(10, 2) NULL,
    description VARCHAR(512) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_package_store FOREIGN KEY (store_id) REFERENCES store (id)
);

CREATE TABLE tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_tag_code UNIQUE (code)
);

CREATE TABLE store_tag_rel (
    store_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (store_id, tag_id),
    CONSTRAINT fk_store_tag_store FOREIGN KEY (store_id) REFERENCES store (id),
    CONSTRAINT fk_store_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id)
);

CREATE TABLE shelf_application (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_org_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL,
    reject_reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shelf_app_store FOREIGN KEY (store_id) REFERENCES store (id),
    CONSTRAINT fk_shelf_app_org FOREIGN KEY (merchant_org_id) REFERENCES merchant_org (id),
    CONSTRAINT fk_shelf_app_reviewer FOREIGN KEY (reviewed_by) REFERENCES admin_user (id)
);

CREATE INDEX idx_shelf_app_status ON shelf_application (status, created_at);
CREATE INDEX idx_shelf_app_store ON shelf_application (store_id);

CREATE TABLE booking_intent (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    intent_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    merchant_org_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NULL,
    nights INT NULL,
    guests INT NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    user_remark VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_intent_no UNIQUE (intent_no),
    CONSTRAINT fk_intent_user FOREIGN KEY (user_id) REFERENCES portal_user (id),
    CONSTRAINT fk_intent_store FOREIGN KEY (store_id) REFERENCES store (id),
    CONSTRAINT fk_intent_org FOREIGN KEY (merchant_org_id) REFERENCES merchant_org (id)
);

CREATE INDEX idx_intent_user ON booking_intent (user_id, created_at);
CREATE INDEX idx_intent_org_status ON booking_intent (merchant_org_id, status);
CREATE INDEX idx_intent_status_updated ON booking_intent (status, updated_at);

CREATE TABLE intent_note (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    intent_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intent_note_intent FOREIGN KEY (intent_id) REFERENCES booking_intent (id),
    CONSTRAINT fk_intent_note_admin FOREIGN KEY (admin_id) REFERENCES admin_user (id)
);

CREATE TABLE article (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    cover_url VARCHAR(512) NULL,
    summary VARCHAR(500) NULL,
    body TEXT NULL,
    status VARCHAR(32) NOT NULL,
    region_id BIGINT NULL,
    published_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_article_region FOREIGN KEY (region_id) REFERENCES region (id),
    CONSTRAINT fk_article_author FOREIGN KEY (created_by) REFERENCES admin_user (id)
);

CREATE INDEX idx_article_status_pub ON article (status, published_at DESC);
CREATE INDEX idx_article_region ON article (region_id);

CREATE TABLE site_config (
    config_key VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value VARCHAR(2000) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    CONSTRAINT fk_site_config_admin FOREIGN KEY (updated_by) REFERENCES admin_user (id)
);

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_type VARCHAR(32) NOT NULL,
    actor_id BIGINT NULL,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NULL,
    target_id BIGINT NULL,
    detail_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created ON audit_log (created_at DESC);
CREATE INDEX idx_audit_target ON audit_log (target_type, target_id);
