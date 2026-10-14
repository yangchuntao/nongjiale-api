-- B6：首页推荐楼层写在房源上，不另建推荐位资源。
-- 只追加列。已有行默认不推荐；下面两家已上架样例供本地首页演示。

ALTER TABLE homestay ADD COLUMN home_recommended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE homestay ADD COLUMN home_recommend_sort INT NOT NULL DEFAULT 0;

UPDATE homestay SET home_recommended = TRUE, home_recommend_sort = 1 WHERE name = '长兴鸿玖农家乐';
UPDATE homestay SET home_recommended = TRUE, home_recommend_sort = 2 WHERE name = '宜兴翠竹昀民宿';
