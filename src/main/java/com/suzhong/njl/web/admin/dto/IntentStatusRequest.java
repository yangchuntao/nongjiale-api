package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.IntentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record IntentStatusRequest(
        @NotNull(message = "目标状态不能为空")
        IntentStatus targetStatus,
        @Size(max = 1000, message = "备注过长")
        String note,
        @NotNull(message = "版本不能为空")
        Long version) {
}
