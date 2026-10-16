package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.NoteAction;

public record IntentNoteItem(
        Long id,
        Long adminId,
        NoteAction action,
        IntentStatus fromStatus,
        IntentStatus toStatus,
        String note,
        LocalDateTime createdAt) {
}
