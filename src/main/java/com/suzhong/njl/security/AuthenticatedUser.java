package com.suzhong.njl.security;

import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.LoginChannel;
import com.suzhong.njl.domain.enums.UserType;

/**
 * 已登录主体在内存令牌中的快照，也是 Spring Security 的 {@code principal}。
 *
 * <p>职责：把「是谁、属于哪一端、拥有什么权限、归属哪个商家主体」压缩成一个不可变对象，
 * 随 {@link IssuedToken} 一起写入 {@link TokenStore}，每次请求由 {@link BearerTokenFilter} 取出后
 * 放进 {@code SecurityContext}。业务代码通过 {@link CurrentUser} 读取，不直接碰 SecurityContext。
 *
 * <p>生命周期：每条令牌一份，随令牌签发创建、过期或作废时丢弃；record 天然不可变，可安全跨线程共享。
 * 它只是快照——用户改手机号或角色变更后，旧令牌中的字段不会自动刷新，需要重新登录或作废令牌。
 *
 * <p>关键限制：不携带 {@code session_key}、密码哈希等敏感凭据。{@code session_key} 只用于解密微信侧数据，
 * 且一期不解密旧版 {@code encryptedData}，放进令牌既无用途又会扩大泄露面。
 *
 * @param userId       登录用户主键：游客为 {@code portal_user.id}，商家为 {@code merchant_user.id}，
 *                     运营为 {@code admin_user.id}，三端各自独立、不可互相替代
 * @param userType     用户大类，决定可访问的路径前缀
 * @param adminRole    运营角色；仅 {@link UserType#OPERATOR} 时非空，其余为空
 * @param merchantOrgId 商家主体 ID；仅商家令牌非空，用于限定只能操作本主体数据
 * @param displayName  展示名，可能为空（未完善的账号）
 * @param phone        手机号，未绑定或运营账号无此概念时为空
 * @param loginChannel 登录渠道；微信登录为 {@link LoginChannel#WECHAT}，网页版密码登录为空
 * @param wechatOpenid 本次微信登录的 openid；网页版令牌为空，解绑时据此精确作废旧令牌
 */
public record AuthenticatedUser(
        Long userId,
        UserType userType,
        AdminRole adminRole,
        Long merchantOrgId,
        String displayName,
        String phone,
        LoginChannel loginChannel,
        String wechatOpenid) {

    /**
     * 兼容网页版密码登录的六参构造：渠道与 openid 均置空。
     *
     * <p>网页版令牌没有微信身份，因而不能调用绑定/解绑；这两个字段为空正是该限制的来源。
     */
    public AuthenticatedUser(
            Long userId,
            UserType userType,
            AdminRole adminRole,
            Long merchantOrgId,
            String displayName,
            String phone) {
        this(userId, userType, adminRole, merchantOrgId, displayName, phone, null, null);
    }

    /**
     * 计算写入 Spring Security 的权限名（单值，不带 {@code ROLE_} 前缀）。
     *
     * <p>运营取 {@link AdminRole} 的名字（{@code OPERATOR}/{@code AGENT}），游客与商家直接取
     * {@link UserType} 的名字，与 {@code SecurityConfig} 中的 {@code hasAuthority} 规则一一对应。
     *
     * @return 权限名字符串，供 {@code SimpleGrantedAuthority} 使用
     * @throws IllegalStateException 运营身份缺少 {@code adminRole}，属于数据装配错误，不应被静默忽略
     */
    public String authority() {
        if (userType == UserType.OPERATOR) {
            if (adminRole == null) {
                throw new IllegalStateException("运营身份缺少角色");
            }
            return adminRole.name();
        }
        return userType.name();
    }
}
