-- 开城：市的开通态、三项手勾、N/M 阈值。不写假门店、假攻略。
-- 扬州、泰州、南通保持 OPEN，门户与 MVP-0 一致。②③不落库，计数不够也不改回 CLOSED。

ALTER TABLE region ADD COLUMN open_status VARCHAR(32) NULL;

CREATE INDEX idx_region_level_open_status ON region (level, open_status);

CREATE TABLE region_open_checklist (
    city_id BIGINT NOT NULL PRIMARY KEY,
    destination_ready BOOLEAN NOT NULL DEFAULT FALSE,
    support_covered BOOLEAN NOT NULL DEFAULT FALSE,
    dirty_data_cleared BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_region_open_checklist_city FOREIGN KEY (city_id) REFERENCES region (id),
    CONSTRAINT fk_region_open_checklist_admin FOREIGN KEY (updated_by) REFERENCES admin_user (id)
);

UPDATE region
SET open_status = 'OPEN'
WHERE level = 2 AND slug IN ('yangzhou', 'taizhou', 'nantong');

UPDATE region
SET open_status = 'CLOSED'
WHERE level = 2 AND slug NOT IN ('yangzhou', 'taizhou', 'nantong');

INSERT INTO region_open_checklist (
    city_id, destination_ready, support_covered, dirty_data_cleared, version, updated_at, updated_by
)
SELECT id, TRUE, TRUE, TRUE, 0, CURRENT_TIMESTAMP, NULL
FROM region
WHERE level = 2 AND slug IN ('yangzhou', 'taizhou', 'nantong');

INSERT INTO region_open_checklist (
    city_id, destination_ready, support_covered, dirty_data_cleared, version, updated_at, updated_by
)
SELECT id, FALSE, FALSE, FALSE, 0, CURRENT_TIMESTAMP, NULL
FROM region
WHERE level = 2 AND slug NOT IN ('yangzhou', 'taizhou', 'nantong');

INSERT INTO site_config (config_key, config_value, updated_at, updated_by)
VALUES
    ('open_city.min_stores', '5', CURRENT_TIMESTAMP, NULL),
    ('open_city.min_articles', '1', CURRENT_TIMESTAMP, NULL);
