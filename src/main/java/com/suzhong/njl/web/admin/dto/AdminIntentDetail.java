package com.suzhong.njl.web.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.IntentStatus;

public record AdminIntentDetail(
        Long id,
        String intentNo,
        Long userId,
        Long storeId,
        String storeName,
        Long merchantOrgId,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guests,
        String contactName,
        String contactPhone,
        String userRemark,
        IntentStatus status,
        String statusLabel,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<IntentNoteItem> notes) {
}
