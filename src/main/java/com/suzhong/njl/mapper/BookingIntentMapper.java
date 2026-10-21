package com.suzhong.njl.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.BookingIntent;
/**
 * 预订意向 Mapper，对应 {@code booking_intent} 表。
 *
 * <p>实体带 {@code @Version} 乐观锁，运营改状态时按 id + version 更新，
 * 影响行数为 0 即视为并发冲突并返回 409；游客只能查自己的意向，条件由 Service 拼装。
 *
 * <p>自定义 SQL 只有城市看板的聚合一条：按门店地区分组计数，供运营指标下推数据库，
 * 替代「全量拉意向实体再内存归并」的旧实现。
 */
public interface BookingIntentMapper extends BaseMapper<BookingIntent> {

    /**
     * 按门店所属地区聚合窗口内的意向计数。
     *
     * <p>口径与旧内存归并一致：{@code store_id} 为空的意向天然不进结果（join 掉）；
     * 门店被删的意向同样丢弃；状态计数用 {@code COUNT(CASE ...)}，只数当前状态。
     * 地区到市的归属（区县挂父市、挂省不归属）由调用方用 {@code CityRules.owningCityId} 完成。
     *
     * @param from 统计时间下界（含）；null 表示不限制时间（ALL 窗口）
     * @return 每个出现过的 {@code store.region_id} 一行
     */
    @Select("""
            <script>
            SELECT s.region_id AS regionId,
                   COUNT(*) AS intentCount,
                   COUNT(CASE WHEN i.status = 'CONFIRMED' THEN 1 END) AS confirmedCount,
                   COUNT(CASE WHEN i.status = 'CHECKED_IN' THEN 1 END) AS checkedInCount
            FROM booking_intent i
            JOIN store s ON s.id = i.store_id
            WHERE i.store_id IS NOT NULL
            <if test="from != null">AND i.created_at &gt;= #{from}</if>
            GROUP BY s.region_id
            </script>
            """)
    List<RegionIntentAggregate> aggregateByRegion(@Param("from") LocalDateTime from);
}
