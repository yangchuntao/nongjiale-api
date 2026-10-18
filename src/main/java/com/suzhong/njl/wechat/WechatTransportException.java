package com.suzhong.njl.wechat;

/**
 * 出站失败。消息里不带 URL，避免 AppSecret、access_token 进日志。
 */
public class WechatTransportException extends RuntimeException {

    public WechatTransportException() {
        super("wechat-transport");
    }
}
