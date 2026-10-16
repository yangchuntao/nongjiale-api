package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

public record AdminMerchantResponse(
        Long id,
        String name,
        String contactName,
        String contactPhone,
        MerchantOrgStatus status,
        LocalDateTime createdAt,
        List<AdminMerchantUserItem> users) {
}
