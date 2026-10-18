package com.suzhong.njl.wechat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

    @Override
    public WechatHttpResult exchange(String method, URI uri, String body) {
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
            Thread.currentThread().interrupt();
            throw new WechatTransportException();
        } catch (IOException ex) {
            throw new WechatTransportException();
        }
    }
}
