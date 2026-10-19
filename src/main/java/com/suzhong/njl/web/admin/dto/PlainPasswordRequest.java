package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 可选明文密码请求体，入驻通过（{@code POST /api/admin/merchant-applies/{id}/approve}）
 * 与重置密码（{@code POST /api/admin/merchants/{orgId}/reset-password}）共用。
 *
 * <p>整个请求体可以为空：{@code password} 不传、为 null 或空白时，服务端生成 10 位随机密码。
 * 传了就必须满足 8 到 64 位，否则返回 400「密码长度须为 8 到 64 位」。
 *
 * @param password 明文密码，可空；8 到 64 位。不会落库，只存哈希，明文仅在当次响应返回
 */
@Schema(name = "PlainPasswordRequest", description = "可选明文密码请求体，入驻审核通过与重置商家密码共用；整个请求体可以为空")
public record PlainPasswordRequest(
        @Schema(description = "明文密码，可空，8 到 64 位；不传、为 null 或空白时服务端生成 10 位随机密码，长度不符返回 400；不会落库，只存哈希")
        @Size(min = 8, max = 64, message = "密码长度须为 8 到 64 位")
        String password) {
}
