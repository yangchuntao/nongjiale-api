package com.suzhong.njl.mapper;

/**
 * 城市意向聚合行：{@code booking_intent} 按门店所属地区（{@code store.region_id}）分组后的计数快照。
 *
 * <p>仅供 {@code BookingIntentMapper#aggregateByRegion} 的查询结果映射使用：
 * 归属市、时间窗过滤都在 SQL 里完成或由服务层二次归并，这里只承载 SQL 的输出列。
 */
public class RegionIntentAggregate {

    /** 门店所属地区 ID（可能是市本身或区县），由服务层经 {@code CityRules.owningCityId} 归属到市。 */
    private Long regionId;

    /** 窗口内全部意向数，含已改期。 */
    private long intentCount;

    /** 当前状态为已确认的意向数。 */
    private long confirmedCount;

    /** 当前状态为已入住的意向数。 */
    private long checkedInCount;

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public long getIntentCount() {
        return intentCount;
    }

    public void setIntentCount(long intentCount) {
        this.intentCount = intentCount;
    }

    public long getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(long confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public long getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(long checkedInCount) {
        this.checkedInCount = checkedInCount;
    }
}
