package com.suzhong.njl.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.enums.WechatMessage;

class WechatMessageTest {

    @Test
    void everySentenceCarriesItsHttpStatus() {
        assertThat(WechatMessage.NOT_CONFIGURED.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(WechatMessage.NOT_CONFIGURED.text()).isEqualTo("微信登录未配置");
        assertThat(WechatMessage.CODE_EXPIRED.text()).isEqualTo("微信登录已失效，请重试");
        assertThat(WechatMessage.PHONE_REQUIRED_FOR_INTENT.text())
                .isEqualTo("请先绑定手机号，完成与网页版账号打通后再提交意向");
        assertThat(WechatMessage.SHELL_DISPLAY_NAME).isEqualTo("微信用户");
        for (WechatMessage message : WechatMessage.values()) {
            assertThat(message.text()).isNotBlank();
            assertThat(message.toException().getStatus()).isEqualTo(message.status());
            assertThat(message.toException().getMessage()).isEqualTo(message.text());
        }
    }
}
