package com.suzhong.njl.web.admin.dto;

public record AdminMerchantUserItem(
        Long id,
        String phone,
        String displayName,
        Boolean enabled) {
}
