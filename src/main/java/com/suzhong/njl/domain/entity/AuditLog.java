package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.ActorType;
import com.suzhong.njl.domain.enums.AuditAction;

/**
 * 运营操作审计流水，对应 {@code audit_log} 表。
 *
 * <p>只追加、不更新、不删除，用于追溯入驻审核、上下架、意向状态、开城、站点配置和
 * Banner 变更。当前写入方是运营后台（{@code ActorType.ADMIN}），没有对外查询接口。
 */
@TableName("audit_log")
public class AuditLog {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作者类型，枚举 {@link ActorType}：{@code ADMIN} 运营、{@code MERCHANT} 商家、{@code SYSTEM} 系统。 */
    private ActorType actorType;

    /** 操作者 ID，按 {@code actorType} 指向 {@code admin_user.id} 或 {@code merchant_user.id}；系统操作可空。 */
    private Long actorId;

    /** 动作类型，枚举 {@link AuditAction}，如 {@code APPLY_APPROVE}、{@code SHELF_FORCE}、{@code CITY_OPEN}。 */
    private AuditAction action;

    /** 目标类型码，取自 {@link com.suzhong.njl.domain.enums.AuditTarget}，如 {@code STORE}、{@code BOOKING_INTENT}；可空。 */
    private String targetType;

    /** 目标主键，配合 {@code targetType} 定位记录；站点配置等无单条目标的动作可空。 */
    private Long targetId;

    /** 变更明细 JSON 对象字符串，可空；序列化失败时写入 {@code null}，不影响主操作。 */
    private String detailJson;

    /** 记录时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
