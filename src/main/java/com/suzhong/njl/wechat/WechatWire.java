package com.suzhong.njl.wechat;

import java.net.URI;

/**
 * 微信出站传输的最小抽象，只负责发请求与收响应。
 *
 * <p>职责：把 HTTP 细节与业务解析解耦。生产实现是 {@link JdkWechatWire}；
 * 测试可注入桩实现来模拟超时、非 2xx、异常 JSON 等场景，无需真实网络。
 *
 * <p>关键限制：实现必须线程安全（单例复用），且不得自行重试——微信的 code 类凭证一次性，
 * 重试只会放大失败。失败以 {@link WechatTransportException} 抛出，不返回 {@code null}。
 */
public interface WechatWire {

    /**
     * 发送一次 HTTP 请求。
     *
     * @param method HTTP 方法，约定为 {@code GET} 或 {@code POST}
     * @param uri    完整目标地址，不能为空且必须指向微信开放接口域名
     * @param body   请求体；GET 或空正文时为 {@code null}
     * @return 原始状态码与响应体，非 2xx 也正常返回
     * @throws WechatTransportException 网络失败、超时、线程中断或地址不合规
     */
    WechatHttpResult exchange(String method, URI uri, String body);
}
