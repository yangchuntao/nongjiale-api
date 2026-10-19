package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.NoteAction;

/**
 * 意向跟进记录，对应 {@code intent_note} 表。
 *
 * <p>由运营在后台追加，用于保存状态变更轨迹和纯备注；只增不改，按创建时间正序展示，
 * 与 {@code booking_intent} 的当前状态共同构成意向处理历史。
 */
@TableName("intent_note")
public class IntentNote {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属意向 ID，对应 {@code booking_intent.id}，非空。 */
    private Long intentId;

    /** 操作运营 ID，对应 {@code admin_user.id}，非空。 */
    private Long adminId;

    /** 记录类型，枚举 {@link NoteAction}：{@code STATUS_CHANGE} 状态变更、{@code COMMENT} 仅备注。 */
    private NoteAction action;

    /** 变更前状态，枚举 {@link IntentStatus}；{@code COMMENT} 类型可空。 */
    private IntentStatus fromStatus;

    /** 变更后状态，枚举 {@link IntentStatus}；{@code COMMENT} 类型可空。 */
    private IntentStatus toStatus;

    /** 备注正文，可空，最长 1000 字；状态变更时可附原因或沟通结论。 */
    private String note;

    /** 记录时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public NoteAction getAction() {
        return action;
    }

    public void setAction(NoteAction action) {
        this.action = action;
    }

    public IntentStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(IntentStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public IntentStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(IntentStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
