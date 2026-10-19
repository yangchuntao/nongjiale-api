package com.suzhong.njl.wechat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * {@link WechatWire} 的 JDK HttpClient 实现，真正发出 HTTPS 请求。
 *
 * <p>职责：只做「发请求、收响应」，不做 JSON 解析、不做业务错误码映射，这些留在
 * {@link HttpWechatMiniappClient}。单独抽一层是为了可测试性：测试可以注入假的 {@code WechatWire}
 * 或自定义 {@link HttpClient}，无需真的访问微信，也无需启动网络。
 *
 * <p>生命周期：随 {@link HttpWechatMiniappClient} 单例创建一次；内部 {@link HttpClient} 线程安全，
 * 可被并发请求复用。{@code HttpWechatMiniappClient} 也允许通过包内构造器注入其他实现。
 *
 * <p>关键限制：
 * <ul>
 *   <li>连接与读取超时均为 3 秒，超时/IO 异常统一抛 {@link WechatTransportException}；</li>
 *   <li>禁止跟随重定向，防止凭证被 3xx 带到非微信域名；</li>
 *   <li>只允许 {@code https://api.weixin.qq.com}，其他 scheme 或 host 直接拒绝。</li>
 * </ul>
 */
final class JdkWechatWire implements WechatWire {

    private final HttpClient httpClient;

    JdkWechatWire() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    JdkWechatWire(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 发送一次请求并返回状态码与响应体。
     *
     * <p>仅支持 GET 与 POST；POST 固定 {@code application/json; charset=UTF-8}，正文为空时发送空串。
     * 响应体始终按 UTF-8 解码，微信返回的中文 errmsg 不会乱码。
     *
     * @param method HTTP 方法，仅识别 {@code POST}，其余按 GET 处理
     * @param uri    目标地址，必须为 {@code https} 且 host 为 {@code api.weixin.qq.com}
     * @param body   请求体，可为 {@code null}（按空串发送）
     * @return HTTP 状态码与响应体原文；非 2xx 也会正常返回，由调用方判断
     * @throws WechatTransportException 地址不合规、连接/读取超时、IO 失败或线程被中断
     */
    @Override
    public WechatHttpResult exchange(String method, URI uri, String body) {
        // 出站白名单：防止配置或拼接错误把 AppSecret/access_token 发到其他主机
        if (uri == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || !"api.weixin.qq.com".equalsIgnoreCase(uri.getHost())) {
            throw new WechatTransportException();
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3));
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new WechatHttpResult(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            // 恢复中断标记，交由上层线程模型决定是否终止；对外只暴露传输失败
            Thread.currentThread().interrupt();
            throw new WechatTransportException();
        } catch (IOException ex) {
            throw new WechatTransportException();
        }
    }
}
