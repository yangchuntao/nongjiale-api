package com.suzhong.njl.domain.enums;

/**
 * 登录用户的大类，只用于内存令牌与 Spring Security 的权限判定，不落库。
 *
 * <p>没有 {@code @EnumValue}。{@code AuthenticatedUser.authority()} 会把它转成权限名：
 * 运营取 {@link AdminRole} 的名字（{@code OPERATOR} 或 {@code AGENT}），
 * 游客与商家分别取 {@code TOURIST}、{@code MERCHANT}。
 * 各端路径前缀与权限的对应关系见 {@code SecurityConfig}。
 */
public enum UserType {
    /** 游客：门户端用户，令牌只能访问 {@code /api/auth/**}、{@code /api/me/**} 与提交意向。 */
    TOURIST,
    /** 运营：后台账号，权限名实际取 {@link AdminRole}，可访问 {@code /api/admin/**}。 */
    OPERATOR,
    /** 商家：商家端账号，只能访问 {@code /api/merchant/**} 且限定本主体数据。 */
    MERCHANT
}
