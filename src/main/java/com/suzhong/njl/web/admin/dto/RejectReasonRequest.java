package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record RejectReasonRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因过长")
        String reason) {
}
