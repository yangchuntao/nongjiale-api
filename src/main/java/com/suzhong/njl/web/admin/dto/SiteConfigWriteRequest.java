package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record SiteConfigWriteRequest(
        @NotBlank(message = "站名不能为空")
        @Size(max = 64, message = "站名过长")
        String siteName,
        @NotBlank(message = "主色不能为空")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "主色格式不正确")
        String brandColor,
        @NotBlank(message = "客服电话不能为空")
        @Size(max = 32, message = "客服电话过长")
        String contactPhone) {
}
