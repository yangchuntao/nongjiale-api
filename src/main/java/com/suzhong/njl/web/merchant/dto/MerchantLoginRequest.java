package com.suzhong.njl.web.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 商家登录请求体（{@code POST /api/merchant/auth/login}，公开接口）。
 *
 * <p>手机号或密码错误统一返回 401「手机号或密码错误」；账号被停用返回 403「账号已停用」，
 * 所属商家主体非启用状态返回 403「商家已停用」。未知字段会被拒绝。
 *
 * @param phone    商家账号手机号，必填，须匹配 {@code ^1\d{10}$}
 * @param password 登录密码，必填，明文传输，服务端用 BCrypt 比对
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record MerchantLoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        String password) {
}
