package com.nongjiale.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 一期内存 Token；有仓库后可换成 JWT / Redis，接口契约不变。 */
@Component
public class SessionTokens {
    private final Map<String, AuthPrincipal> tokens = new ConcurrentHashMap<>();

    public String issue(AuthPrincipal principal) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, principal);
        return token;
    }

    public Optional<AuthPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return Optional.ofNullable(tokens.get(token));
    }

    public void revoke(String token) {
        tokens.remove(token);
    }
}
