package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.ReviewStatus;

/**
 * 商家入驻申请，对应 {@code merchant_apply} 表。
 *
 * <p>门户访客免登录提交，运营在后台审核：通过时创建 {@code merchant_org} 与
 * {@code merchant_user} 并回填 {@code merchantOrgId}；驳回必须填原因。
 *
 * <p>状态流转仅允许 {@code PENDING → APPROVED} 或 {@code PENDING → REJECTED}，
 * 已审核的申请再次审核返回 409。
 */
@TableName("merchant_apply")
public class MerchantApply {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家主体名称，非空，最长 128 字；审核通过后作为 {@code merchant_org.name}。 */
    private String orgName;

    /** 联系人姓名，可空，最长 64 字；通过后写入主体联系人，并作为商家账号展示名的优先取值。 */
    private String contactName;

    /** 联系电话，可空，最长 20 字；填写时须匹配 {@code ^1\d{10}$}。审核通过时该号码即商家登录手机号，必填且不得重复。 */
    private String contactPhone;

    /** 计划门店名，可空，最长 128 字；仅供运营参考，不会自动创建门店。 */
    private String plannedStoreName;

    /** 意向地区 ID，对应 {@code region.id}，可空；提交时校验地区存在且启用。 */
    private Long regionId;

    /** 商家介绍，可空，最长 1000 字。 */
    private String description;

    /** 资质图片 URL 文本，可空；当前门户提交接口不接收该字段，主要由运营补录或历史数据使用。 */
    private String licenseUrls;

    /** 来源，{@code PORTAL}=商家在门户自填（默认），{@code ADMIN}=运营代录入；入库时写入，不可变。 */
    private String source;

    /** 审核状态，枚举 {@link ReviewStatus}：{@code PENDING} 待审、{@code APPROVED} 已通过、{@code REJECTED} 已驳回。 */
    private ReviewStatus status;

    /** 驳回原因，可空；仅 {@code REJECTED} 时有值，驳回接口要求非空。 */
    private String rejectReason;

    /** 审核人 ID，对应 {@code admin_user.id}，可空；未审核时为 {@code null}。 */
    private Long reviewedBy;

    /** 审核时间，可空；通过或驳回时写入应用当前时间。 */
    private LocalDateTime reviewedAt;

    /** 审核通过后创建（或关联）的商家主体 ID，对应 {@code merchant_org.id}，可空。 */
    private Long merchantOrgId;

    /** 提交时间，插入时自动填充；后台队列按此倒序排列。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getPlannedStoreName() {
        return plannedStoreName;
    }

    public void setPlannedStoreName(String plannedStoreName) {
        this.plannedStoreName = plannedStoreName;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicenseUrls() {
        return licenseUrls;
    }

    public void setLicenseUrls(String licenseUrls) {
        this.licenseUrls = licenseUrls;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getMerchantOrgId() {
        return merchantOrgId;
    }

    public void setMerchantOrgId(Long merchantOrgId) {
        this.merchantOrgId = merchantOrgId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
