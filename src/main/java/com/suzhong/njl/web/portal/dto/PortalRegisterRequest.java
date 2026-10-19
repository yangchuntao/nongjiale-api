package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 游客注册请求体（{@code POST /api/auth/register}，公开接口）。
 *
 * <p>注册成功即签发游客令牌；手机号全局唯一，已注册返回 409「手机号已注册」。
 * 未知字段会被拒绝。
 *
 * @param phone       手机号，必填，须匹配 {@code ^1\d{10}$}；同时作为登录账号
 * @param password    登录密码，必填，长度 6 到 64 位；服务端 BCrypt 存储，不落明文
 * @param displayName 展示名，可空，最长 64 字；空白时用脱敏手机号作为默认展示名
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PortalRegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度为 6 到 64 位")
        String password,
        @Size(max = 64, message = "昵称过长")
        String displayName) {
}
