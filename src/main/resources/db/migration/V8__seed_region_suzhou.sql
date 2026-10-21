-- 新增苏州市及其区县。只写区划，不开城、不写门店、不写攻略。
-- 开城仍由运营在后台按市手动开通；本脚本只保证苏州市出现在开城列表里可进入详情页。
-- 排序号接在苏中三市（10/20/30）之后，与既有种子风格一致。
-- 幂等：按 slug 判重，重复执行不会插入第二份。

INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT id, 'suzhou', '苏州市', 2, 40, TRUE, 'SUZHOU'
FROM region
WHERE slug = 'jiangsu'
  AND NOT EXISTS (SELECT 1 FROM region WHERE slug = 'suzhou');

-- 苏州市下辖区县。开城单位仍是市，这里只为门户按区县筛选预留，不单独开城。
INSERT INTO region (parent_id, slug, name, level, sort_order, enabled, seed_market)
SELECT p.id, t.slug, t.name, 3, t.sort_order, TRUE, 'SUZHOU'
FROM region p
JOIN (
    SELECT 'gusu' AS slug, '姑苏区' AS name, 41 AS sort_order
    UNION ALL SELECT 'wuzhong', '吴中区', 42
    UNION ALL SELECT 'xiangcheng', '相城区', 43
    UNION ALL SELECT 'wujiang', '吴江区', 44
    UNION ALL SELECT 'huqiu', '虎丘区', 45
) t
WHERE p.slug = 'suzhou'
  AND NOT EXISTS (SELECT 1 FROM region r WHERE r.slug = t.slug);

-- 开城清单初始行：三项手勾全为 false，等运营在详情页勾选。
-- open_status 保持 NULL（等价 CLOSED），不预设开通。
INSERT INTO region_open_checklist (
    city_id, destination_ready, support_covered, dirty_data_cleared, version, updated_at, updated_by
)
SELECT id, FALSE, FALSE, FALSE, 0, CURRENT_TIMESTAMP, NULL
FROM region
WHERE slug = 'suzhou'
  AND level = 2
  AND NOT EXISTS (SELECT 1 FROM region_open_checklist c WHERE c.city_id = region.id);
