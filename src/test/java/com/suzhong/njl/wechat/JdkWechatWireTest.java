package com.suzhong.njl.wechat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class JdkWechatWireTest {

    @Test
    void rejectsAnythingOtherThanTheWechatHost() {
        JdkWechatWire wire = new JdkWechatWire();
        assertThatThrownBy(() -> wire.exchange("GET", null, null))
                .isInstanceOf(WechatTransportException.class)
                .hasMessage("wechat-transport");
        assertThatThrownBy(() -> wire.exchange("GET", URI.create("http://api.weixin.qq.com/sns/jscode2session"), null))
                .isInstanceOf(WechatTransportException.class);
        assertThatThrownBy(() -> wire.exchange("GET", URI.create("https://example.com/sns/jscode2session?secret=super-secret"), null))
                .isInstanceOf(WechatTransportException.class)
                .hasMessageNotContaining("super-secret");
    }

    @Test
    void postAndGetReturnTheStatusWithoutWrappingTheCause() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"errcode\":0}");
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return response;
        });
        JdkWechatWire wire = new JdkWechatWire(httpClient);
        URI uri = URI.create("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=atk");

        WechatHttpResult result = wire.exchange("POST", uri, "{\"code\":\"c\"}");

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body()).contains("errcode");
        assertThat(captured.get().method()).isEqualTo("POST");

        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return response;
        });
        wire.exchange("GET", URI.create("https://api.weixin.qq.com/cgi-bin/token?secret=super-secret"), null);
        assertThat(captured.get().method()).isEqualTo("GET");
    }

    @Test
    void networkFailuresDoNotExposeTheSecret() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new HttpTimeoutException("https://api.weixin.qq.com/?secret=super-secret"));
        JdkWechatWire wire = new JdkWechatWire(httpClient);
        assertThatThrownBy(() -> wire.exchange(
                "GET",
                URI.create("https://api.weixin.qq.com/cgi-bin/token?secret=super-secret"),
                null))
                .isInstanceOf(WechatTransportException.class)
                .hasMessageNotContaining("super-secret")
                .hasNoCause();

        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException("stop"));
        try {
            assertThatThrownBy(() -> wire.exchange(
                    "GET",
                    URI.create("https://api.weixin.qq.com/sns/jscode2session"),
                    null))
                    .isInstanceOf(WechatTransportException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("reset"));
        assertThatThrownBy(() -> wire.exchange(
                "GET",
                URI.create("https://api.weixin.qq.com/sns/jscode2session"),
                null))
                .isInstanceOf(WechatTransportException.class)
                .hasNoCause();
    }
}
