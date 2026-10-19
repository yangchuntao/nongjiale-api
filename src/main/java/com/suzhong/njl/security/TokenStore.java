package com.suzhong.njl.security;

import java.util.Optional;

/**
 * Bearer 令牌存放处。一期只允许进程内实现，禁止写入 Redis。
 *
 * <p>契约：实现必须是线程安全的；{@link #issue(AuthenticatedUser)} 不得返回空，
 * {@link #resolve(String)} 对任何非法输入都只返回空 {@link Optional}，不得抛异常。
 *
 * <p>关键限制：令牌状态只存在于单个进程，重启即全部失效，多实例之间不共享；
 * 不要为了「多实例共享」改成 Redis，这会破坏一期「令牌只放内存」的安全约定。
 */
public interface TokenStore {

    /**
     * 为用户签发一枚新令牌。
     *
     * <p>是否作废同一用户的旧令牌由实现决定；{@link InMemoryTokenStore} 允许并存。
     *
     * @param user 登录主体，不能为空
     * @return 令牌原文与绝对过期时刻，非空
     */
    IssuedToken issue(AuthenticatedUser user);

    /**
     * 把令牌原文解析为登录主体。
     *
     * <p>允许传入 {@code null} 或空白；令牌不存在、已过期、已作废时统一返回空，不抛异常。
     * 过期记录的具体清理时机由实现决定，调用方不应依赖「解析一次就立刻释放内存」。
     *
     * @param token 令牌原文，不含 {@code Bearer } 前缀，可为 {@code null}
     * @return 有效令牌返回主体，否则为空
     */
    Optional<AuthenticatedUser> resolve(String token);

    /**
     * 丢掉该商家主体尚未过期的内存令牌。停用和改密后调用，不写 Redis。
     */
    void revokeMerchantOrg(Long merchantOrgId);

    /**
     * 丢掉带这枚微信 openid 的内存令牌。网页版密码登录令牌的 openid 为空，不会被丢掉。不写 Redis。
     */
    void revokeWechatOpenid(String openid);
}
