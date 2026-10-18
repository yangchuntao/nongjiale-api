package com.suzhong.njl.wechat;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.config.WechatMiniappProperties;
import com.suzhong.njl.domain.enums.WechatMessage;

/**
 * 只访问 api.weixin.qq.com 的三个接口。session_key 与 access_token 不落库、不进日志、不返回。
 */
@Component
public class HttpWechatMiniappClient implements WechatMiniappClient {

    static final String ORIGIN = "https://api.weixin.qq.com";

    private static final Logger log = LoggerFactory.getLogger(HttpWechatMiniappClient.class);

    private final WechatMiniappProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final WechatWire wire;
    private final Object tokenLock = new Object();
    private volatile CachedToken cachedToken;

    @Autowired
    public HttpWechatMiniappClient(WechatMiniappProperties properties, Clock clock, ObjectMapper objectMapper) {
        this(properties, clock, objectMapper, new JdkWechatWire());
    }

    HttpWechatMiniappClient(
            WechatMiniappProperties properties,
            Clock clock,
            ObjectMapper objectMapper,
            WechatWire wire) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.wire = wire;
    }

    @Override
    public JsCodeSession codeToSession(String jsCode) {
        requireConfigured();
        String query = "appid=" + enc(properties.normalizedAppId())
                + "&secret=" + enc(properties.normalizedSecret())
                + "&js_code=" + enc(jsCode)
                + "&grant_type=authorization_code";
        JsonNode root = get("/sns/jscode2session?" + query);
        String openid = text(root, "openid");
        if (openid.isEmpty()) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        String unionid = text(root, "unionid");
        return new JsCodeSession(openid, unionid.isEmpty() ? null : unionid);
    }

    @Override
    public PurePhoneNumber phoneNumber(String phoneCode) {
        requireConfigured();
        String token = accessToken();
        String body = writePhoneBody(phoneCode);
        JsonNode root = post("/wxa/business/getuserphonenumber?access_token=" + enc(token), body);
        JsonNode info = root.path("phone_info");
        String country = info.path("countryCode").asText("");
        if (country.isBlank()) {
            country = "86";
        }
        if (!"86".equals(country)) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        String pure = info.path("purePhoneNumber").asText("");
        if (!pure.matches("^1\\d{10}$")) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        return new PurePhoneNumber(pure);
    }

    private String accessToken() {
        Instant now = Instant.now(clock);
        CachedToken current = cachedToken;
        if (current != null && current.expiresAt().isAfter(now)) {
            return current.value();
        }
        synchronized (tokenLock) {
            now = Instant.now(clock);
            current = cachedToken;
            if (current != null && current.expiresAt().isAfter(now)) {
                return current.value();
            }
            FetchedToken fetched = fetchToken();
            long ttlSeconds = Math.max(1, fetched.expiresIn() - 120L);
            cachedToken = new CachedToken(fetched.value(), now.plusSeconds(ttlSeconds));
            return fetched.value();
        }
    }

    private FetchedToken fetchToken() {
        String query = "grant_type=client_credential&appid=" + enc(properties.normalizedAppId())
                + "&secret=" + enc(properties.normalizedSecret());
        JsonNode root = get("/cgi-bin/token?" + query);
        String token = text(root, "access_token");
        int expiresIn = root.path("expires_in").asInt(0);
        if (token.isEmpty() || expiresIn <= 0) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        return new FetchedToken(token, expiresIn);
    }

    private String writePhoneBody(String phoneCode) {
        try {
            return objectMapper.writeValueAsString(Map.of("code", phoneCode));
        } catch (Exception ex) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
    }

    private JsonNode get(String pathAndQuery) {
        return parseSuccess(exchange("GET", pathAndQuery, null));
    }

    private JsonNode post(String pathAndQuery, String body) {
        return parseSuccess(exchange("POST", pathAndQuery, body));
    }

    private WechatHttpResult exchange(String method, String pathAndQuery, String body) {
        URI uri;
        try {
            uri = URI.create(ORIGIN + pathAndQuery);
        } catch (IllegalArgumentException ex) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        try {
            return wire.exchange(method, uri, body);
        } catch (WechatTransportException ex) {
            log.warn("微信网络失败");
            throw WechatMessage.UNAVAILABLE.toException();
        }
    }

    private JsonNode parseSuccess(WechatHttpResult result) {
        if (result == null || result.status() < 200 || result.status() >= 300) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(result.body() == null ? "" : result.body());
        } catch (Exception ex) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        int errcode = 0;
        JsonNode err = root.get("errcode");
        if (err != null && !err.isNull()) {
            errcode = err.asInt(Integer.MIN_VALUE);
        }
        if (errcode != 0) {
            log.warn("微信接口错误 errcode={}", errcode);
            throw map(errcode).toException();
        }
        return root;
    }

    private WechatMessage map(int errcode) {
        if (errcode == 40013) {
            return WechatMessage.NOT_CONFIGURED;
        }
        if (errcode == 40029 || errcode == 40163) {
            return WechatMessage.CODE_EXPIRED;
        }
        if (errcode == -1) {
            return WechatMessage.UNAVAILABLE;
        }
        if (errcode >= 40000 && errcode < 50000) {
            return WechatMessage.CODE_EXPIRED;
        }
        return WechatMessage.UNAVAILABLE;
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw WechatMessage.NOT_CONFIGURED.toException();
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private record CachedToken(String value, Instant expiresAt) {
    }

    private record FetchedToken(String value, int expiresIn) {
    }
}
