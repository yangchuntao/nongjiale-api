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

/**
 * 门店（房源），对应 {@code store} 表。
 *
 * <p>平台的核心供给，归属一个商家主体和一个地区。商家端可编辑本主体门店但不能直接改
 * 上下架和首页推荐；运营可代建、代编并直接改 {@code shelfStatus}；门户只读已开通城市内
 * 的上架门店。
 *
 * <p>关键约定：
 * <ul>
 *   <li>套餐文案以 {@code packageText} 为权威，{@code store_package} 可以没有行，
 *       二者冲突时以 {@code packageText} 为准；</li>
 *   <li>{@code testData=true} 表示抓取或导入的数据，门户会带「测试」标记，验收精选店必须为
 *       {@code false}；</li>
 *   <li>{@code shelfStatus} 与所属市开通态共同决定门户可见性，下架或未开通详情返回 410。</li>
 * </ul>
 */
@TableName("store")
public class Store {

    /** 主键，自增；被意向、上下架申请、标签关联等引用。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属商家主体 ID，对应 {@code merchant_org.id}，非空；运营代建门店时也必须绑定。 */
    private Long merchantOrgId;

    /** 所属地区 ID，对应 {@code region.id}，非空；种子数据落到区县，也可挂市；挂省或未知层级不作为可订对象。 */
    private Long regionId;

    /** 门店名称，非空，最长 128 字；列表关键词按此字段包含匹配。 */
    private String name;

    /** 详细地址，可空，最长 256 字。 */
    private String address;

    /** 联系电话，可空，最长 32 字；按字符串存储，允许座机或含分隔符的写法。 */
    private String phone;

    /** 封面图 URL，可空，最长 512 字；只存 URL。 */
    private String coverUrl;

    /** 相册图片 URL 的 JSON 数组字符串，可空，如 {@code ["https://..."]}；解析失败按空相册处理，不影响详情。 */
    private String galleryJson;

    /** 价格展示文案，可空，最长 64 字，如「¥268起」；仅用于展示，不参与排序和筛选。 */
    private String priceText;

    /** 起步价，单位元，DECIMAL(10,2)，可空；用于列表价格带筛选和价格排序，为空时不会命中价格条件。 */
    private BigDecimal priceFrom;

    /** 套餐权威文案，TEXT，可空。详情与验收认这一列；与 {@code store_package} 冲突时以本字段为准，没有套餐明细行也属正常。 */
    private String packageText;

    /** 设施说明，可空，TEXT；当前为中文顿号分隔的自由文本（如「独立卫浴、空调」），不是 JSON 或 CSV。 */
    private String facilities;

    /** 特色说明，可空，TEXT；当前为中文顿号分隔的自由文本（如「运河边、家常菜」），不是 JSON 或 CSV。 */
    private String features;

    /** 入住须知，可空，TEXT；门户按原文本展示。 */
    private String notice;

    /** 交通指引，可空，TEXT；门户按原文本展示。 */
    private String trafficText;

    /** 上下架状态，枚举 {@link ShelfStatus}：{@code ON_SHELF} 上架、{@code OFF_SHELF} 下架、{@code DRAFT} 草稿；仅上架且所属市开通时门户可见。 */
    private ShelfStatus shelfStatus;

    /** 是否首页推荐，非空，默认 {@code false}；首页 feed 只取推荐且上架、所属市已开通的门店。 */
    private Boolean homeRecommended;

    /** 首页推荐排序，非空，默认 100；升序排列，数字越小越靠前。 */
    private Integer homeSort;

    /** 是否为抓取/导入的测试数据，非空，默认 {@code false}；为 {@code true} 时门户列表和详情带「测试」标记，验收精选店保持 {@code false}。 */
    private Boolean testData;

    /** 乐观锁版本号，非空，默认 0；商家和运营更新时必须回传，冲突时返回 409。 */
    @Version
    private Long version;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最后更新时间，插入和更新时自动填充。 */
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
