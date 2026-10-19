package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.NoteAction;

/**
 * 意向跟进记录（{@code GET /api/admin/intents/{id}} 的 notes 元素，
 * 状态变更与追加备注接口也随详情整体返回）。
 *
 * <p>只追加不删除，按记录 ID 从早到晚返回；状态变更由运营改状态时自动写入，
 * 纯文字备注走 {@code POST /api/admin/intents/{id}/notes}。
 *
 * @param id         记录 ID，对应 {@code intent_note.id}
 * @param adminId    操作运营 ID
 * @param action     动作类型，取值见 {@link NoteAction}：STATUS_CHANGE（状态变更）、COMMENT（备注）
 * @param fromStatus 变更前状态，仅 STATUS_CHANGE 有值，否则为 null
 * @param toStatus   变更后状态，仅 STATUS_CHANGE 有值，否则为 null
 * @param note       说明文字，可空；状态变更时是运营填的备注，纯备注时是正文
 * @param createdAt  记录时间，{@code Asia/Shanghai} 的 ISO-8601
 */
public record IntentNoteItem(
        Long id,
        Long adminId,
        NoteAction action,
        IntentStatus fromStatus,
        IntentStatus toStatus,
        String note,
        LocalDateTime createdAt) {
}
