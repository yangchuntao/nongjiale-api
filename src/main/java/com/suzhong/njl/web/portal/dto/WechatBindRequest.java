package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 微信账号绑定请求体（{@code POST /api/auth/wechat/bind}，须微信渠道的游客 Bearer）。
 *
 * <p>两种模式互斥、二选一：传 {@code phoneCode}（微信手机号凭证），或传 {@code phone} + {@code password}；
 * 两者同时出现或都不给都返回 400。手机号尚未注册时 {@code password} 必填并作为新账号密码（6 到 64 位）；
 * 手机号已存在时密码用于校验，不传则仅凭微信手机号凭证完成合并。未知字段会被拒绝。
 *
 * @param phoneCode 微信手机号凭证，凭证模式下非空，去空白后长度不超过 128
 * @param phone     手填手机号，手填模式下非空，须匹配 {@code ^1\d{10}$}
 * @param password  密码，可空；凭证模式通常不传，手填模式必填，长度 6 到 64 位
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(name = "WechatBindRequest", description = "微信账号绑定请求体，POST /api/auth/wechat/bind，须微信渠道的游客 Bearer；phoneCode 与 phone+password 两种模式互斥二选一")
public record WechatBindRequest(
        @Schema(description = "微信手机号凭证，凭证模式下非空，去空白后长度不超过 128")
        String phoneCode,
        @Schema(description = "手填手机号，手填模式下非空，须匹配 ^1\\d{10}$")
        String phone,
        @Schema(description = "密码，可空；凭证模式通常不传，手填模式必填，长度 6 到 64 位")
        String password) {
}
