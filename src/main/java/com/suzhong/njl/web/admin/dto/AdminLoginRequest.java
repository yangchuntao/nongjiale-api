package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 运营登录请求体（{@code POST /api/admin/auth/login}）。
 *
 * <p>该接口是 {@code /api/admin/**} 下唯一免 Bearer 的接口；校验失败统一返回 400，
 * 提示取 Bean Validation 的第一条字段错误（全局异常处理器行为）。
 * 未知字段直接拒绝，返回 400「请求体格式不正确」。
 *
 * @param phone    登录手机号，必填；须匹配 {@code ^1\d{10}$}，为空报「手机号不能为空」，格式不符报「手机号格式不正确」
 * @param password 登录密码，必填原文；为空报「密码不能为空」；账号不存在或密码错误返回 401「手机号或密码错误」，账号停用返回 403
 */
@Schema(name = "AdminLoginRequest", description = "运营登录请求体，用于 POST /api/admin/auth/login")
public record AdminLoginRequest(
        @Schema(description = "登录手机号，必填，须匹配 ^1\\d{10}$；为空报「手机号不能为空」，格式不符报「手机号格式不正确」")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @Schema(description = "登录密码，必填原文；为空报「密码不能为空」，账号不存在或密码错误返回 401，账号停用返回 403")
        @NotBlank(message = "密码不能为空")
        String password) {
}
