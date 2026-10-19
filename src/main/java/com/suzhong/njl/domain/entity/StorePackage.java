package com.suzhong.njl.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 门店套餐明细，对应 {@code store_package} 表。
 *
 * <p>可选明细，允许门店没有任何行。门店详情与验收以 {@code store.package_text} 为准，
 * 两处冲突时本表不覆盖主文案；本表只用于结构化的套餐名称、价格和说明展示。
 */
@TableName("store_package")
public class StorePackage {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属门店 ID，对应 {@code store.id}，非空。 */
    private Long storeId;

    /** 套餐名称，非空，最长 64 字，如「标间」。 */
    private String name;

    /** 套餐起步价，单位元，DECIMAL(10,2)，可空。 */
    private BigDecimal priceFrom;

    /** 套餐说明，可空，最长 512 字，如「仅床位，不含餐」。 */
    private String description;

    /** 展示顺序，非空，默认 0，升序排列。 */
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPriceFrom() {
        return priceFrom;
    }

    public void setPriceFrom(BigDecimal priceFrom) {
        this.priceFrom = priceFrom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

}
