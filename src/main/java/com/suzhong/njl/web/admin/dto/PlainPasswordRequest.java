package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record PlainPasswordRequest(
        @Size(min = 8, max = 64, message = "密码长度须为 8 到 64 位")
        String password) {
}
