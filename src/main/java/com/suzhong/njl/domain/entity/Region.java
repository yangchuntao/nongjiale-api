package com.suzhong.njl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.SeedMarket;
import com.suzhong.njl.domain.support.RegionLevelTypeHandler;

@TableName("region")
public class Region {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String slug;

    private String name;

    @TableField(typeHandler = RegionLevelTypeHandler.class)
    private RegionLevel level;

    private Integer sortOrder;

    private Boolean enabled;

    /** 只对市有意义。省、区县保持空。空在门户上按未开通处理。 */
    private RegionOpenStatus openStatus;

    private SeedMarket seedMarket;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RegionLevel getLevel() {
        return level;
    }

    public void setLevel(RegionLevel level) {
        this.level = level;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public RegionOpenStatus getOpenStatus() {
        return openStatus;
    }

    public void setOpenStatus(RegionOpenStatus openStatus) {
        this.openStatus = openStatus;
    }

    public SeedMarket getSeedMarket() {
        return seedMarket;
    }

    public void setSeedMarket(SeedMarket seedMarket) {
        this.seedMarket = seedMarket;
    }

}
