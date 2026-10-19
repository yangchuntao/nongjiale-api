package com.suzhong.njl.web.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 微信打通状态（{@code GET /api/auth/wechat/link} 与 {@code POST /api/auth/wechat/unbind} 的响应体，须游客 Bearer）。
 *
 * <p>目标手机号已有账号时，绑定会把微信身份迁移到该账号并换发其令牌；手机号尚未注册时则把当前壳账号
 * 补成正式账号。解绑只删除「当前 AppID + 当前 openid」这一条身份，并作废带该 openid 的内存令牌，
 * 网页版密码令牌继续有效。未配置微信 AppID/Secret 时，登录、绑定、解绑、查询统一返回 503「微信登录未配置」。
 *
 * @param userId        当前游客用户 ID
 * @param phone         手机号，明文不脱敏，可空；未绑定时为 null
 * @param phoneBound    是否已绑定手机号
 * @param wechatBound   当前 AppID 下是否仍有微信身份；解绑后通常为 false
 * @param accountLinked 是否完成账号打通：手机号已绑、密码已设、微信已绑三者同时成立
 */
@Schema(name = "WechatLinkResponse", description = "微信打通状态，GET /api/auth/wechat/link 与 POST /api/auth/wechat/unbind 的响应体，须游客 Bearer；未配置微信 AppID/Secret 时统一返回 503")
public record WechatLinkResponse(
        @Schema(description = "当前游客用户 ID")
        Long userId,
        @Schema(description = "手机号，明文不脱敏，可空；未绑定时为 null")
        String phone,
        @Schema(description = "是否已绑定手机号")
        boolean phoneBound,
        @Schema(description = "当前 AppID 下是否仍有微信身份；解绑后通常为 false")
        boolean wechatBound,
        @Schema(description = "是否完成账号打通：手机号已绑、密码已设、微信已绑三者同时成立")
        boolean accountLinked) {
}
