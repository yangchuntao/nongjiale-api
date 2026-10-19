package com.suzhong.njl.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.StoreSort;

/**
 * 门店列表查询条件载体，不是实体、也不对应表。
 *
 * <p>由门户 {@code StoreQueryService} 组装后传给 {@code StoreMapper.searchOnShelf} 与
 * {@code countOnShelf}，两个方法共用同一份条件，保证列表与总数口径一致。
 *
 * <p>关键约束：{@code orderBy} 会原样拼进 SQL 的 ORDER BY 子句，
 * 只能通过 {@link #applySort} 用枚举映射写入，禁止 setter 或外部参数直填。
 */
public class StoreSearch {

    /** 上架状态过滤条件，默认 {@code ON_SHELF}；门户列表只会用这个值。 */
    private ShelfStatus shelfStatus = ShelfStatus.ON_SHELF;
    /** 可见地区 ID 集合；为空或 null 时 SQL 不追加地区条件，未开通城市由 Service 提前返回空列表。 */
    private List<Long> regionIds;
    /** 标签 code 过滤；空白表示不限，SQL 用 EXISTS 关联标签表并要求标签仍启用。 */
    private String tagCode;
    /** 起步价下限（元），闭区间，对应 {@code store.price_from}；null 表示不限。 */
    private BigDecimal priceMin;
    /** 起步价上限（元），闭区间，对应 {@code store.price_from}；null 表示不限。 */
    private BigDecimal priceMax;
    /** 店名包含匹配关键词；空白表示不限，已由 Service 规范化并转义 {@code %} 与 {@code _}。 */
    private String keyword;
    /** 排序片段，取值为枚举映射出的固定 SQL，默认综合排序；直接拼入 ORDER BY，见类注释。 */
    private String orderBy = StoreSort.COMPREHENSIVE.orderBySql();
    /** 单页条数，由分页参数换算，Service 侧保证 1 到 100；默认 20。 */
    private int limit = 20;
    /** 偏移量，等于 (page - 1) * pageSize；默认 0 表示第一页。 */
    private int offset;

    public ShelfStatus getShelfStatus() {
        return shelfStatus;
    }

    public void setShelfStatus(ShelfStatus shelfStatus) {
        this.shelfStatus = shelfStatus;
    }

    public List<Long> getRegionIds() {
        return regionIds;
    }

    public void setRegionIds(List<Long> regionIds) {
        this.regionIds = regionIds;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getOrderBy() {
        return orderBy;
    }

    /**
     * 设置排序方式，是 {@code orderBy} 的唯一写入口。
     *
     * <p>参数类型限定为 {@code StoreSort} 枚举，枚举内部只返回几个固定的 SQL 片段，
     * 外部 {@code sort} 字符串必须先经枚举校验，因此不存在把任意文本拼进 ORDER BY 的路径，
     * 这也是 {@code ${orderBy}} 能安全使用的白名单依据。
     *
     * @param sort 已解析的排序枚举，不能为 null
     */
    public void applySort(StoreSort sort) {
        this.orderBy = sort.orderBySql();
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
