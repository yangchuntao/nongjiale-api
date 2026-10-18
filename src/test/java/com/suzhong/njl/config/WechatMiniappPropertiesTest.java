package com.suzhong.njl.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WechatMiniappPropertiesTest {

    @Test
    void blankOrMissingValuesAreUnconfigured() {
        WechatMiniappProperties properties = new WechatMiniappProperties();
        assertThat(properties.configured()).isFalse();

        properties.setAppId("  ");
        properties.setSecret(null);
        assertThat(properties.configured()).isFalse();
        assertThat(properties.normalizedAppId()).isEmpty();
        assertThat(properties.normalizedSecret()).isEmpty();

        properties.setAppId(" wx-test-app ");
        properties.setSecret(" test-secret ");
        assertThat(properties.configured()).isTrue();
        assertThat(properties.normalizedAppId()).isEqualTo("wx-test-app");
        assertThat(properties.normalizedSecret()).isEqualTo("test-secret");
        assertThat(properties.getAppId()).isEqualTo(" wx-test-app ");
        assertThat(properties.getSecret()).isEqualTo(" test-secret ");
    }
}
