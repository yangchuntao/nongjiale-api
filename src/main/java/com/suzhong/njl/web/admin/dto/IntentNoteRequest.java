package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 意向备注请求体（{@code POST /api/admin/intents/{id}/notes}）。
 *
 * <p>只追加一条 {@code COMMENT} 记录，不改变意向状态；落库前去除首尾空白。
 * 意向不存在返回 404「意向不存在」，未知字段返回 400「请求体格式不正确」。
 *
 * @param note 备注正文，必填，最长 1000 字；为空或全空白报「备注不能为空」，超长报「备注过长」
 */
public record IntentNoteRequest(
        @NotBlank(message = "备注不能为空")
        @Size(max = 1000, message = "备注过长")
        String note) {
}
