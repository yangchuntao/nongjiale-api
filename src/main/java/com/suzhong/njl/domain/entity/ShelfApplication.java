package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;

/**
 * 门店上下架申请，对应 {@code shelf_application} 表。
 *
 * <p>商家端发起上架或下架申请，运营后台审核；通过后由运营服务真正修改
 * {@code store.shelf_status}，驳回则门店可见性不变。运营代上下架走
 * {@code PUT /api/admin/stores/{id}}，不落本表，而是直接生效并记 {@code SHELF_FORCE} 审计。
 *
 * <p>同一门店同时只允许一条 {@code PENDING} 申请，该约束由应用层保证，数据库未做部分唯一索引。
 */
@TableName("shelf_application")
public class ShelfApplication {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请门店 ID，对应 {@code store.id}，非空。 */
    private Long storeId;

    /** 门店归属商家主体 ID，对应 {@code merchant_org.id}，非空；用于商家端按主体过滤。 */
    private Long merchantOrgId;

    /** 申请动作，枚举 {@link ShelfAction}：{@code PUBLISH} 申请上架、{@code UNPUBLISH} 申请下架。 */
    private ShelfAction action;

    /** 商家申请理由，可空，最长 500 字。 */
    private String reason;

    /** 审核状态，枚举 {@link ReviewStatus}：{@code PENDING} 待审、{@code APPROVED} 已通过、{@code REJECTED} 已驳回。 */
    private ReviewStatus status;

    /** 驳回原因，可空，最长 500 字；驳回接口要求非空。 */
    private String rejectReason;

    /** 审核人 ID，对应 {@code admin_user.id}，可空；未审核时为 {@code null}。 */
    private Long reviewedBy;

    /** 审核时间，可空；通过或驳回时写入应用当前时间。 */
    private LocalDateTime reviewedAt;

    /** 提交时间，插入时自动填充；审核队列按此倒序排列。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

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

    public Long getMerchantOrgId() {
        return merchantOrgId;
    }

    public void setMerchantOrgId(Long merchantOrgId) {
        this.merchantOrgId = merchantOrgId;
    }

    public ShelfAction getAction() {
        return action;
    }

    public void setAction(ShelfAction action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
