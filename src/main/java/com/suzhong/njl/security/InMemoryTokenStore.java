package com.suzhong.njl.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.suzhong.njl.config.AppProperties;

@Component
public class InMemoryTokenStore implements TokenStore {

    private final AppProperties appProperties;
    private final Clock applicationClock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, TokenRecord> tokens = new ConcurrentHashMap<>();

    public InMemoryTokenStore(AppProperties appProperties, Clock applicationClock) {
        this.appProperties = appProperties;
        this.applicationClock = applicationClock;
    }

    @Override
    public IssuedToken issue(AuthenticatedUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = java.util.HexFormat.of().formatHex(bytes);
        Instant expiresAt = Instant.now(applicationClock).plus(appProperties.getSecurity().getTokenTtl());
        tokens.put(token, new TokenRecord(user, expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    @Override
    public Optional<AuthenticatedUser> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        TokenRecord record = tokens.get(token);
        if (record == null) {
            return Optional.empty();
        }
        if (!record.expiresAt().isAfter(Instant.now(applicationClock))) {
            tokens.remove(token, record);
            return Optional.empty();
        }
        return Optional.of(record.user());
    }

    private record TokenRecord(AuthenticatedUser user, Instant expiresAt) {
    }
}
