package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record MerchantApplyRequest(
        @NotBlank(message = "请填写主体名称")
        @Size(max = 128, message = "主体名称过长")
        String orgName,
        @Size(max = 64, message = "联系人过长")
        String contactName,
        @Size(max = 20, message = "联系电话过长")
        String contactPhone,
        @Size(max = 128, message = "门店名过长")
        String plannedStoreName,
        Long regionId,
        @Size(max = 1000, message = "介绍过长")
        String description) {
}
