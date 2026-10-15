package com.suzhong.njl.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.config.AppProperties;
import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.UserType;

class InMemoryTokenStoreTest {

    @Test
    void expiredTokenIsDroppedAndNeverLeavesTheJvm() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-15T01:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("Asia/Shanghai");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTokenTtl(Duration.ofMinutes(30));
        InMemoryTokenStore store = new InMemoryTokenStore(properties, clock);
        AuthenticatedUser user = new AuthenticatedUser(1L, UserType.OPERATOR, AdminRole.OPERATOR, null, "苏中运营", "13800000000");

        IssuedToken issued = store.issue(user);
        assertThat(store.resolve(issued.token())).contains(user);

        now.set(now.get().plus(Duration.ofMinutes(31)));
        assertThat(store.resolve(issued.token())).isEmpty();
    }
}
