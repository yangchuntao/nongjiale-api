package com.suzhong.njl.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.StoreSort;

public class StoreSearch {

    private ShelfStatus shelfStatus = ShelfStatus.ON_SHELF;
    private List<Long> regionIds;
    private String tagCode;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String keyword;
    private String orderBy = StoreSort.COMPREHENSIVE.orderBySql();
    private int limit = 20;
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
