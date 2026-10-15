package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.NoteAction;

@TableName("intent_note")
public class IntentNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long intentId;

    private Long adminId;

    private NoteAction action;

    private IntentStatus fromStatus;

    private IntentStatus toStatus;

    private String note;

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
