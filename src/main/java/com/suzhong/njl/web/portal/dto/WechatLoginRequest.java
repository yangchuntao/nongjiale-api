package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 微信小程序静默登录请求体（{@code POST /api/auth/wechat/login}，公开接口）。
 *
 * <p>code 由小程序 {@code wx.login} 获取，服务端换取 openid 与 session_key；session_key 在
 * 微信客户端层即丢弃，不落库、不写日志、不出现在响应里。code 缺失或空白返回 400
 * 「请提供微信登录凭证」，超过 128 字返回 400「请求体格式不正确」，微信侧换取失败返回 502。
 * 未知字段会被拒绝。
 *
 * @param code 微信登录凭证，必填，非空白，长度不超过 128；一次性使用，过期需重新获取
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record WechatLoginRequest(String code) {
}
