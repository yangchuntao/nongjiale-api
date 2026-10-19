package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 攻略新建与全量更新请求体（{@code POST /api/admin/articles}、
 * {@code PUT /api/admin/articles/{id}}）。
 *
 * <p>更新是全量覆盖，未传的可空字段会被清空；保存为 PUBLISHED 后门户立即可见，
 * DRAFT 对门户表现为列表不出现、详情 404。未知字段返回 400「请求体格式不正确」。
 *
 * @param title    标题，必填，最长 200；为空报「标题不能为空」，超长报「标题过长」
 * @param coverUrl 封面图 URL，可空，最长 512，超长报「封面地址过长」
 * @param summary  摘要，可空，最长 500，超长报「摘要过长」
 * @param body     正文，可空，无长度上限校验
 * @param status   发布状态，必填，取值见 {@link ArticleStatus}：DRAFT、PUBLISHED；为空报「状态不能为空」
 * @param regionId 所属地区 ID，可空；传了但地区不存在返回 400「地区不存在」，不校验是否启用
 */
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
