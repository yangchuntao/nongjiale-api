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
        AuthenticatedUser user = new AuthenticatedUser(1L, UserType.OPERATOR, AdminRole.OPERATOR, null, "苏中运营", "17606838920");

        IssuedToken issued = store.issue(user);
        assertThat(store.resolve(issued.token())).contains(user);

        now.set(now.get().plus(Duration.ofMinutes(31)));
        assertThat(store.resolve(issued.token())).isEmpty();
    }

    @Test
    void revokeMerchantOrgDropsOnlyThatOrg() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setTokenTtl(Duration.ofHours(1));
        InMemoryTokenStore store = new InMemoryTokenStore(properties, Clock.systemUTC());
        AuthenticatedUser merchant = new AuthenticatedUser(9L, UserType.MERCHANT, null, 3L, "商家", "13700000000");
        AuthenticatedUser other = new AuthenticatedUser(8L, UserType.MERCHANT, null, 4L, "别家", "13700000001");
        AuthenticatedUser admin = new AuthenticatedUser(1L, UserType.OPERATOR, AdminRole.OPERATOR, null, "运营", "17606838920");

        IssuedToken dropped = store.issue(merchant);
        IssuedToken kept = store.issue(other);
        IssuedToken adminToken = store.issue(admin);
        store.revokeMerchantOrg(3L);

        assertThat(store.resolve(dropped.token())).isEmpty();
        assertThat(store.resolve(kept.token())).contains(other);
        assertThat(store.resolve(adminToken.token())).contains(admin);
    }
}
