-- 数据库版本与区划数据现状
SELECT '--- flyway ---' AS section;
SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT '--- region ---' AS section;
SELECT COUNT(*) AS total, SUM(level = 1) AS provinces, SUM(level = 2) AS cities,
       SUM(level = 3) AS districts, SUM(open_status = 'OPEN') AS opened
FROM region;
SELECT '--- adcode/migration check ---' AS section;
SELECT COUNT(*) AS with_adcode FROM region WHERE adcode IS NOT NULL;
