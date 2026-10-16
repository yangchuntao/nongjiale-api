package com.suzhong.njl.web.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ShelfAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ShelfApplicationRequest(
        @NotNull(message = "上下架动作不能为空")
        ShelfAction action,
        @Size(max = 500, message = "申请原因过长")
        String reason) {
}
