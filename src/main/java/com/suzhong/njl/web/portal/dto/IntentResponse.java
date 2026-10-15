package com.suzhong.njl.web.portal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;

public record IntentResponse(
        Long id,
        String intentNo,
        IntentStatus status,
        Long storeId,
        String storeName,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guests,
        String contactName,
        String contactPhone,
        String userRemark,
        LocalDateTime createdAt) {
}
