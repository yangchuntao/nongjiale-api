package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record IntentNoteRequest(
        @NotBlank(message = "备注不能为空")
        @Size(max = 1000, message = "备注过长")
        String note) {
}
