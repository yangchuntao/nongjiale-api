package com.suzhong.njl.wechat;

/**
 * 出站失败。消息里不带 URL，避免 AppSecret、access_token 进日志。
 *
 * <p>语义：只表示「请求没有拿到可解析的 HTTP 响应」——连接/读取超时、DNS 或 TLS 失败、
 * 线程中断、目标地址不在白名单等。微信返回 4xx/5xx 或业务 errcode 不属于本异常，
 * 那些由 {@link HttpWechatMiniappClient} 正常解析后映射。
 *
 * <p>生命周期：每次失败新建，属于轻量运行时异常，不填充调用栈之外的信息；
 * 由客户端捕获后统一转为 502，不向上层暴露网络细节。
 */
public class WechatTransportException extends RuntimeException {

    /** 固定文案，不拼接原始 URL、请求参数或底层异常消息，防止敏感信息随日志扩散。 */
    public WechatTransportException() {
        super("wechat-transport");
    }
}
