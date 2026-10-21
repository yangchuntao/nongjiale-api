package com.suzhong.njl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.SeedMarket;
import com.suzhong.njl.domain.support.RegionLevelTypeHandler;

/**
 * 地区树节点，对应 {@code region} 表。
 *
 * <p>按省（1）→ 市（2）→ 区县（3）三级组织，供门户筛选、门店归属和开城判断使用；
 * 数据由种子脚本和运营侧维护，不提供运行时增删接口。
 *
 * <p>关键约定：只有市一级有开通态 {@code openStatus}；省和区县保持为空，空值在门户上
 * 视同未开通。市对客可见要求自身启用且 {@code openStatus=OPEN}；区县还要求自身启用、
 * 父市已开通；直挂省的门店不作为可订对象。具体规则收敛在 {@code CityRules} /
 * {@code CityVisibility}。
 */
@TableName("region")
public class Region {

    /** 主键，自增；被门店、攻略、Banner、入驻申请等引用。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级地区 ID，对应 {@code region.id}；省为空，市指向省，区县指向市。 */
    private Long parentId;

    /** 地区英文标识，全局唯一，最长 64 字，如 {@code jiangsu}、{@code yangzhou}；用于种子定位和前台路由。 */
    private String slug;

    /** 地区中文名，非空，最长 64 字，如「扬州」「广陵」。 */
    private String name;

    /** 层级，枚举 {@link RegionLevel}，经 {@link RegionLevelTypeHandler} 以 TINYINT 存库：1 省、2 市、3 区县。 */
    @TableField(typeHandler = RegionLevelTypeHandler.class)
    private RegionLevel level;

    /** 同级展示顺序，非空，升序排列。 */
    private Integer sortOrder;

    /** 是否启用，非空；停用后不出现在门户地区列表，也不能作为门店或入驻申请的归属；门店可见性另按所属市开通态判断。 */
    private Boolean enabled;

    /** 对客开通态，枚举 {@link RegionOpenStatus}：{@code OPEN} 已开通、{@code CLOSED} 未开通。只对市有意义，省、区县保持空；空在门户上按未开通处理。 */
    private RegionOpenStatus openStatus;

    /** 种子市场标识，枚举 {@link SeedMarket}，可空；苏中种子数据为 {@code SUZHONG}，全国同步导入的区划为 null。 */
    private SeedMarket seedMarket;

    /** 国家标准行政区划代码，如 {@code 320500}（苏州市）；全局唯一，是区划同步的幂等键。存量种子可能为空。 */
    private String adcode;

    /** 数据来源，{@link RegionSource} 的字符串码：{@code SEED} 迁移种子、{@code SYNC} 区划同步。 */
    private String source;

    /** 地区简称，如「江苏省」→「江苏」；同步导入时由全称推导，可空。 */
    private String shortName;

    /** 政府驻地经度，可空；来自数据源 center，不参与业务判断。 */
    private java.math.BigDecimal centerLng;

    /** 政府驻地纬度，可空；来自数据源 center，不参与业务判断。 */
    private java.math.BigDecimal centerLat;

    public String getAdcode() {
        return adcode;
    }

    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public java.math.BigDecimal getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(java.math.BigDecimal centerLng) {
        this.centerLng = centerLng;
    }

    public java.math.BigDecimal getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(java.math.BigDecimal centerLat) {
        this.centerLat = centerLat;
    }


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
