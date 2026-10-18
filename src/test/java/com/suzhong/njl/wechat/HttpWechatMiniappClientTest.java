package com.suzhong.njl.wechat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.config.WechatMiniappProperties;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.wechat.WechatMiniappClient.JsCodeSession;
import com.suzhong.njl.wechat.WechatMiniappClient.PurePhoneNumber;

class HttpWechatMiniappClientTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-18T00:00:00Z"));
    private final ScriptedWire wire = new ScriptedWire();
    private WechatMiniappProperties properties;
    private HttpWechatMiniappClient client;

    @BeforeEach
    void setUp() {
        properties = new WechatMiniappProperties();
        properties.setAppId("wx-test-app");
        properties.setSecret("test-secret");
        client = new HttpWechatMiniappClient(properties, clock(), new ObjectMapper(), wire);
    }

    @Test
    void codeToSessionDropsSessionKeyAndKeepsUnionid() {
        wire.session = json(200, """
                {"openid":"openid-1","session_key":"SESSIONKEYSECRET","unionid":"union-1"}
                """);

        JsCodeSession session = client.codeToSession("js-code");

        assertThat(session.openid()).isEqualTo("openid-1");
        assertThat(session.unionid()).isEqualTo("union-1");
        assertThat(session.toString()).doesNotContain("SESSIONKEYSECRET");
        assertThat(wire.paths).containsExactly("/sns/jscode2session");
        assertThat(wire.queries.get(0)).contains("js_code=js-code").contains("grant_type=authorization_code");
        assertThat(wire.queries.get(0)).doesNotContain("session_key");
    }

    @Test
    void missingUnionidStaysNull() {
        wire.session = json(200, "{\"openid\":\"openid-1\",\"session_key\":\"SESSIONKEYSECRET\"}");
        assertThat(client.codeToSession("js-code").unionid()).isNull();
    }

    @Test
    void unconfiguredDoesNotCallWechat() {
        properties.setSecret("  ");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.NOT_CONFIGURED.text());
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.NOT_CONFIGURED.text());
        assertThat(wire.paths).isEmpty();
    }

    @Test
    void wechatErrorCodesMapToTheContract() {
        assertError(40029, WechatMessage.CODE_EXPIRED);
        assertError(40163, WechatMessage.CODE_EXPIRED);
        assertError(40013, WechatMessage.NOT_CONFIGURED);
        assertError(-1, WechatMessage.UNAVAILABLE);
        assertError(40226, WechatMessage.CODE_EXPIRED);
        assertError(50001, WechatMessage.UNAVAILABLE);
    }

    @Test
    void transportAndHttpFailuresAreBadGateway() {
        wire.session = null;
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());

        wire.failTransport = true;
        wire.session = json(200, "{\"openid\":\"openid-1\"}");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());
        wire.failTransport = false;

        wire.session = json(500, "{\"errcode\":0,\"openid\":\"openid-1\"}");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());

        wire.session = json(200, "not-json");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());

        wire.session = json(200, "{\"errcode\":0}");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());
    }

    @Test
    void phoneNumberAcceptsMainlandNumbersAndCachesAccessToken() {
        wire.token = json(200, "{\"access_token\":\"atk-1\",\"expires_in\":200}");
        wire.phone = json(200, """
                {"errcode":0,"phone_info":{"purePhoneNumber":"13900001111","countryCode":""}}
                """);

        PurePhoneNumber first = client.phoneNumber("phone-code");
        PurePhoneNumber second = client.phoneNumber("phone-code");

        assertThat(first.purePhoneNumber()).isEqualTo("13900001111");
        assertThat(second.purePhoneNumber()).isEqualTo("13900001111");
        assertThat(wire.paths.stream().filter(path -> path.equals("/cgi-bin/token")).count()).isEqualTo(1);
        assertThat(wire.bodies).contains("{\"code\":\"phone-code\"}");

        now.set(now.get().plus(Duration.ofSeconds(81)));
        client.phoneNumber("phone-code");
        assertThat(wire.paths.stream().filter(path -> path.equals("/cgi-bin/token")).count()).isEqualTo(2);
    }

    @Test
    void shortLivedTokenStillRefreshesAfterOneSecond() {
        wire.token = json(200, "{\"access_token\":\"atk-1\",\"expires_in\":100}");
        wire.phone = json(200, """
                {"errcode":0,"phone_info":{"purePhoneNumber":"13900001111","countryCode":"86"}}
                """);
        client.phoneNumber("phone-code");
        now.set(now.get().plus(Duration.ofSeconds(2)));
        client.phoneNumber("phone-code");
        assertThat(wire.paths.stream().filter(path -> path.equals("/cgi-bin/token")).count()).isEqualTo(2);
    }

    @Test
    void phoneAndTokenFailures() {
        wire.token = json(200, "{\"errcode\":40013,\"errmsg\":\"invalid appid\"}");
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.NOT_CONFIGURED.text());

        wire.token = json(200, "{\"access_token\":\"\",\"expires_in\":7200}");
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());

        wire.token = json(200, "{\"access_token\":\"atk\",\"expires_in\":0}");
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());

        wire.token = json(200, "{\"access_token\":\"atk\",\"expires_in\":7200}");
        wire.phone = json(200, """
                {"errcode":0,"phone_info":{"purePhoneNumber":"13900001111","countryCode":"1"}}
                """);
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.PHONE_INVALID.text());

        wire.phone = json(200, """
                {"errcode":0,"phone_info":{"purePhoneNumber":"12345","countryCode":"86"}}
                """);
        assertThatThrownBy(() -> client.phoneNumber("phone-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.PHONE_INVALID.text());
    }

    private void assertError(int errcode, WechatMessage expected) {
        wire.session = json(200, "{\"errcode\":" + errcode + ",\"errmsg\":\"secret-context\"}");
        assertThatThrownBy(() -> client.codeToSession("js-code"))
                .isInstanceOf(ApiException.class)
                .hasMessage(expected.text())
                .hasMessageNotContaining("secret-context");
    }

    private Clock clock() {
        return new Clock() {
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
    }

    private static WechatHttpResult json(int status, String body) {
        return new WechatHttpResult(status, body);
    }

    private static final class ScriptedWire implements WechatWire {
        private final List<String> paths = new ArrayList<>();
        private final List<String> queries = new ArrayList<>();
        private final List<String> bodies = new ArrayList<>();
        private WechatHttpResult session = json(200, "{\"openid\":\"openid-1\"}");
        private WechatHttpResult token = json(200, "{\"access_token\":\"atk\",\"expires_in\":7200}");
        private WechatHttpResult phone = json(200, "{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13900001111\",\"countryCode\":\"86\"}}");
        private boolean failTransport;

        @Override
        public WechatHttpResult exchange(String method, URI uri, String body) {
            if (failTransport) {
                throw new WechatTransportException();
            }
            paths.add(uri.getPath());
            queries.add(uri.getRawQuery());
            bodies.add(body);
            String path = uri.getPath();
            if (path.endsWith("/sns/jscode2session")) {
                return session;
            }
            if (path.endsWith("/cgi-bin/token")) {
                return token;
            }
            if (path.endsWith("/wxa/business/getuserphonenumber")) {
                return phone;
            }
            throw new IllegalStateException(path);
        }
    }
}
