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
 *
 * <p>职责：封装微信小程序服务端协议——
 * <ul>
 *   <li>{@code GET /sns/jscode2session}：用 {@code wx.login} 的 code 换 openid（不解密用户资料）；</li>
 *   <li>{@code GET /cgi-bin/token}：用 {@code client_credential} 换 access_token；</li>
 *   <li>{@code POST /wxa/business/getuserphonenumber}：用手机号组件的一次性 code 换纯手机号。</li>
 * </ul>
 *
 * <p>生命周期与线程安全：Spring 单例。{@code access_token} 缓存在实例字段
 * {@link #cachedToken}（{@code volatile}），刷新时用 {@link #tokenLock} 做双重检查，
 * 保证同一时刻只有一个线程发起换取请求，其余线程复用结果。
 *
 * <p>关键限制：
 * <ul>
 *   <li>AppID/Secret 未配置时直接抛 503「微信登录未配置」，不发起任何出站请求；</li>
 *   <li>HTTP 超时 3 秒，失败不自动重试——{@code js_code} 与手机号 code 均为一次性凭证，重试没有意义；</li>
 *   <li>网络异常、非 2xx、响应不是 JSON 统一映射为 502「微信服务暂不可用」；</li>
 *   <li>日志只记 errcode，不打印 URL、Secret、access_token 与响应正文。</li>
 * </ul>
 */
@Component
public class HttpWechatMiniappClient implements WechatMiniappClient {

    /** 微信开放接口固定域名；出站只允许访问这里，避免凭证被带到其他主机。 */
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

    /**
     * 用 {@code wx.login} 的 code 换取 openid。
     *
     * <p>请求 {@code /sns/jscode2session}，参数为 {@code appid}、{@code secret}、{@code js_code}、
     * {@code grant_type=authorization_code}；全部经 URL 编码。微信响应中的 {@code session_key}
     * 不读取、不保存、不返回，本方法只取 openid 与可选的 unionid。
     *
     * @param jsCode 小程序 {@code wx.login} 返回的一次性 code，调用方保证非空且未使用过
     * @return openid 必非空；unionid 未返回时为 {@code null}
     * @throws com.suzhong.njl.web.ApiException 未配置 503；code 无效/已用/过期 401；网络或上游异常 502
     */
    @Override
    public JsCodeSession codeToSession(String jsCode) {
        requireConfigured();
        // 参数顺序与微信文档一致；secret 只出现在本次请求的查询串里，不进日志
        String query = "appid=" + enc(properties.normalizedAppId())
                + "&secret=" + enc(properties.normalizedSecret())
                + "&js_code=" + enc(jsCode)
                + "&grant_type=authorization_code";
        JsonNode root = get("/sns/jscode2session?" + query);
        String openid = text(root, "openid");
        // 成功响应也可能缺 openid，无法定位用户时按上游不可用处理，不签发无主体令牌
        if (openid.isEmpty()) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        String unionid = text(root, "unionid");
        return new JsCodeSession(openid, unionid.isEmpty() ? null : unionid);
    }

    /**
     * 用手机号组件的一次性 code 换取微信绑定的纯手机号。
     *
     * <p>流程：先取（或复用缓存的）access_token，再 {@code POST /wxa/business/getuserphonenumber}，
     * 正文只有一个 {@code code} 字段，值为手机号组件返回的一次性凭证。只接受国家码 86（字段缺失时按 86 处理），
     * 且 {@code purePhoneNumber} 必须匹配 {@code ^1\d{10}$}；其他国家或格式一律按手机号非法处理。
     *
     * @param phoneCode 手机号组件返回的 code，一次性，调用方保证非空
     * @return 大陆 11 位纯手机号，不含国家码与分隔符
     * @throws com.suzhong.njl.web.ApiException 未配置 503；号码非大陆或格式错误 400；
     *                                          code 失效 401；网络或上游异常 502
     */
    @Override
    public PurePhoneNumber phoneNumber(String phoneCode) {
        requireConfigured();
        String token = accessToken();
        String body = writePhoneBody(phoneCode);
        JsonNode root = post("/wxa/business/getuserphonenumber?access_token=" + enc(token), body);
        JsonNode info = root.path("phone_info");
        String country = info.path("countryCode").asText("");
        // 微信未返回国家码时按大陆 86 处理，与前端组件的使用场景一致
        if (country.isBlank()) {
            country = "86";
        }
        if (!"86".equals(country)) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        String pure = info.path("purePhoneNumber").asText("");
        // 再校验一次格式：上游字段缺失或异常时不能把脏号码写进账号
        if (!pure.matches("^1\\d{10}$")) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        return new PurePhoneNumber(pure);
    }

    /**
     * 取 access_token：优先用内存缓存，过期或被并发刷新时走单飞换取。
     *
     * <p>双重检查：先无锁读 {@code volatile} 缓存；未命中再进锁，进锁后重新判断，避免重复换取。
     * 缓存有效期取微信返回的 {@code expires_in} 减 120 秒，提前刷新以规避边界过期。
     *
     * @return 有效 access_token 原文
     */
    private String accessToken() {
        Instant now = Instant.now(clock);
        CachedToken current = cachedToken;
        if (current != null && current.expiresAt().isAfter(now)) {
            return current.value();
        }
        synchronized (tokenLock) {
            // 进锁后重读：可能已被前一个持锁线程刷新
            now = Instant.now(clock);
            current = cachedToken;
            if (current != null && current.expiresAt().isAfter(now)) {
                return current.value();
            }
            FetchedToken fetched = fetchToken();
            // 至少缓存 1 秒，防止微信返回极小 expires_in 时每次都重新换取；提前 120 秒过期
            long ttlSeconds = Math.max(1, fetched.expiresIn() - 120L);
            cachedToken = new CachedToken(fetched.value(), now.plusSeconds(ttlSeconds));
            return fetched.value();
        }
    }

    /**
     * 用 {@code client_credential} 向微信换取 access_token。
     *
     * @return token 原文与微信给出的有效期秒数；字段缺失或非法时抛 502
     */
    private FetchedToken fetchToken() {
        String query = "grant_type=client_credential&appid=" + enc(properties.normalizedAppId())
                + "&secret=" + enc(properties.normalizedSecret());
        JsonNode root = get("/cgi-bin/token?" + query);
        String token = text(root, "access_token");
        int expiresIn = root.path("expires_in").asInt(0);
        // 缺任一字段都视为上游异常，不缓存半成品
        if (token.isEmpty() || expiresIn <= 0) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        return new FetchedToken(token, expiresIn);
    }

    /** 序列化手机号请求体；理论上不会失败，失败时按上游不可用处理，不泄露序列化细节。 */
    private String writePhoneBody(String phoneCode) {
        try {
            return objectMapper.writeValueAsString(Map.of("code", phoneCode));
        } catch (Exception ex) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
    }

    /** 发起 GET 并解析出成功 JSON。 */
    private JsonNode get(String pathAndQuery) {
        return parseSuccess(exchange("GET", pathAndQuery, null));
    }

    /** 发起 POST 并解析出成功 JSON。 */
    private JsonNode post(String pathAndQuery, String body) {
        return parseSuccess(exchange("POST", pathAndQuery, body));
    }

    /**
     * 拼接完整 URL 并交给 {@link WechatWire} 发送。
     *
     * <p>URL 非法或出站失败统一转成 502；异常信息不向上传递，避免把含 secret 的查询串带进日志或响应。
     */
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
            // 只记固定文案：URL 里可能带 secret 或 access_token
            log.warn("微信网络失败");
            throw WechatMessage.UNAVAILABLE.toException();
        }
    }

    /**
     * 校验 HTTP 状态与微信业务错误码。
     *
     * <p>非 2xx、空响应或非 JSON 都按上游不可用处理。微信习惯用 HTTP 200 + {@code errcode} 表达失败，
     * 因此还要检查 {@code errcode}；字段存在但非数字时按未知错误处理（映射为 502）。
     *
     * @param result 出站结果，可为 {@code null}
     * @return 解析后的 JSON 根节点
     */
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
            // 只记录错误码，不记录 errmsg 与请求参数
            log.warn("微信接口错误 errcode={}", errcode);
            throw map(errcode).toException();
        }
        return root;
    }

    /**
     * 把微信 errcode 映射为面向游客的业务异常。
     *
     * <p>规则：40013（AppID 无效）视为配置错误，503；40029/40163（code 无效或已使用）
     * 以及其他 4xxxx（参数/凭证类错误）映射为 401，提示重新登录；
     * -1（微信系统繁忙）与其余未知错误映射为 502。响应体不返回微信原始 errmsg。
     *
     * @param errcode 微信响应中的错误码
     * @return 对应的 {@link WechatMessage} 常量
     */
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
        // 4xxxx 段基本是 code/参数失效，按「微信登录已失效」处理，引导客户端重新 wx.login
        if (errcode >= 40000 && errcode < 50000) {
            return WechatMessage.CODE_EXPIRED;
        }
        return WechatMessage.UNAVAILABLE;
    }

    /** 配置缺失时直接 503，且保证不发起任何出站请求。 */
    private void requireConfigured() {
        if (!properties.configured()) {
            throw WechatMessage.NOT_CONFIGURED.toException();
        }
    }

    /** 查询串统一 UTF-8 百分号编码，避免 code 中的特殊字符破坏 URL。 */
    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 读取字符串字段；字段缺失或为 null 时返回空串，便于调用方直接判空。 */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    /**
     * 内存中的 access_token 缓存项。
     *
     * @param value     微信下发的 access_token 原文
     * @param expiresAt 本地计算的绝对过期时刻（已扣除提前刷新窗口），到期后需重新拉取
     */
    private record CachedToken(String value, Instant expiresAt) {
    }

    /**
     * 微信返回的 access_token 原始信息。
     *
     * @param value     微信下发的 access_token 原文
     * @param expiresIn 微信声明的有效期，单位秒；本地缓存时需预留提前刷新时间
     */
    private record FetchedToken(String value, int expiresIn) {
    }
}
