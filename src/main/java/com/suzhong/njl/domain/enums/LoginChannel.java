package com.suzhong.njl.domain.enums;

/**
 * 内存令牌的登录渠道。网页版密码登录保持空，不写入 JSON。
 *
 * <p>不落库、无 {@code @EnumValue}；只出现在令牌对象的 {@code channel} 字段，
 * 用来限制「微信绑定/解绑」只能对微信渠道的令牌操作。
 */
public enum LoginChannel {
    /** 微信小程序登录：令牌带此渠道，才允许绑定或解绑微信。 */
    WECHAT
}
