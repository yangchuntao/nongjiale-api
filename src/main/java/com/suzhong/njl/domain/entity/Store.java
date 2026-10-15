package com.suzhong.njl.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.Version;
import com.suzhong.njl.domain.enums.ShelfStatus;

@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantOrgId;

    private Long regionId;

    private String name;

    private String address;

    private String phone;

    private String coverUrl;

    private String galleryJson;

    private String priceText;

    private BigDecimal priceFrom;

    /** 套餐权威文案。详情与验收认这一列；与 store_package 冲突时以本字段为准。 */
    private String packageText;

    private String facilities;

    private String features;

    private String notice;

    private String trafficText;

    private ShelfStatus shelfStatus;

    private Boolean homeRecommended;

    private Integer homeSort;

    /** 验收精选店保持 false。抓取或导入的房源必须为 true。 */
    private Boolean testData;

    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantOrgId() {
        return merchantOrgId;
    }

    public void setMerchantOrgId(Long merchantOrgId) {
        this.merchantOrgId = merchantOrgId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getGalleryJson() {
        return galleryJson;
    }

    public void setGalleryJson(String galleryJson) {
        this.galleryJson = galleryJson;
    }

    public String getPriceText() {
        return priceText;
    }

    public void setPriceText(String priceText) {
        this.priceText = priceText;
    }

    public BigDecimal getPriceFrom() {
        return priceFrom;
    }

    public void setPriceFrom(BigDecimal priceFrom) {
        this.priceFrom = priceFrom;
    }

    public String getPackageText() {
        return packageText;
    }

    public void setPackageText(String packageText) {
        this.packageText = packageText;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getTrafficText() {
        return trafficText;
    }

    public void setTrafficText(String trafficText) {
        this.trafficText = trafficText;
    }

    public ShelfStatus getShelfStatus() {
        return shelfStatus;
    }

    public void setShelfStatus(ShelfStatus shelfStatus) {
        this.shelfStatus = shelfStatus;
    }

    public Boolean getHomeRecommended() {
        return homeRecommended;
    }

    public void setHomeRecommended(Boolean homeRecommended) {
        this.homeRecommended = homeRecommended;
    }

    public Integer getHomeSort() {
        return homeSort;
    }

    public void setHomeSort(Integer homeSort) {
        this.homeSort = homeSort;
    }

    public Boolean getTestData() {
        return testData;
    }

    public void setTestData(Boolean testData) {
        this.testData = testData;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
