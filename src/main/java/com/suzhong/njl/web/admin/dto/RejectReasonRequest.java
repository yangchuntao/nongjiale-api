package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 驳回原因请求体，入驻审核驳回与上下架申请驳回共用
 * （{@code POST /api/admin/merchant-applies/{id}/reject}、
 * {@code POST /api/admin/shelf-applications/{id}/reject}）。
 *
 * <p>原因为必填，服务端落库前会去除首尾空白；未知字段返回 400「请求体格式不正确」。
 *
 * @param reason 驳回原因，必填，最长 500 字；为空报「驳回原因不能为空」，超长报「驳回原因过长」
 */
public record RejectReasonRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因过长")
        String reason) {
}
