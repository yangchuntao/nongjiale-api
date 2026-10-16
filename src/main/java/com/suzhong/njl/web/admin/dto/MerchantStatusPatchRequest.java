package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

@JsonIgnoreProperties(ignoreUnknown = false)
public record MerchantStatusPatchRequest(
        MerchantOrgStatus status,
        Boolean enabled) {
}
