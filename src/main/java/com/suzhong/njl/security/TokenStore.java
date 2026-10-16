package com.suzhong.njl.security;

import java.util.Optional;

/**
 * Bearer 令牌存放处。一期只允许进程内实现，禁止写入 Redis。
 */
public interface TokenStore {

    IssuedToken issue(AuthenticatedUser user);

    Optional<AuthenticatedUser> resolve(String token);

    /**
     * 丢掉该商家主体尚未过期的内存令牌。停用和改密后调用，不写 Redis。
     */
    void revokeMerchantOrg(Long merchantOrgId);
}
