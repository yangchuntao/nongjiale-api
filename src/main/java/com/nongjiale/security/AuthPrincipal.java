package com.nongjiale.security;

/**
 * 内存 token 的逻辑 claim。日后换 JWT 时字段名保持：id、subject、type、role、merchantId。
 * 门户：type=TOURIST，role=USER（历史 token 的 role 字面量 TOURIST 仍视为同一门户用户）。
 * 商家：type=MERCHANT，role=MERCHANT，merchantId 必填且只来自服务端绑定。
 */
public record AuthPrincipal(Long id, String subject, String type, String role, Long merchantId) {
    public AuthPrincipal(Long id, String subject, String type, String role) {
        this(id, subject, type, role, null);
    }

    public boolean isTourist() {
        return "TOURIST".equals(type);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(type);
    }

    public boolean isOperator() {
        return isAdmin() && "OPERATOR".equals(role);
    }

    public boolean isAgent() {
        return isAdmin() && "AGENT".equals(role);
    }

    public boolean isMerchant() {
        return "MERCHANT".equals(type) && "MERCHANT".equals(role) && merchantId != null;
    }
}
