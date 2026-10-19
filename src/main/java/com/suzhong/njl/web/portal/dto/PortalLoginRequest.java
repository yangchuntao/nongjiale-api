package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 游客手机号登录请求体（{@code POST /api/auth/login}，公开接口）。
 *
 * <p>账号不存在、账号没有密码（微信壳账号）、密码不匹配三种情况统一返回 401「手机号或密码错误」，
 * 不区分文案，避免被用来探测手机号是否注册。未知字段会被拒绝。
 *
 * @param phone    手机号，必填，须匹配 {@code ^1\d{10}$}（11 位手机号）
 * @param password 登录密码，必填，明文传输；服务端用 BCrypt 比对，不做长度校验以便兼容历史密码
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PortalLoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        String password) {
}
