package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ArticleWriteRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题过长")
        String title,
        @Size(max = 512, message = "封面地址过长")
        String coverUrl,
        @Size(max = 500, message = "摘要过长")
        String summary,
        String body,
        @NotNull(message = "状态不能为空")
        ArticleStatus status,
        Long regionId) {
}
