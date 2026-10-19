package com.suzhong.njl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置，绑定 {@code wechat.miniapp.*}。
 *
 * <p>AppID 与 Secret 只从环境变量 {@code WECHAT_MINIAPP_APP_ID} / {@code WECHAT_MINIAPP_SECRET}
 * 注入，不写进源码。空串视为未配置：进程仍可正常启动，但微信登录、绑定、解绑等接口返回 503
 * 「微信登录未配置」，避免把缺配置变成启动失败。
 */
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatMiniappProperties {

    /** 小程序 AppID，对应 {@code wechat.miniapp.app-id}，环境变量 {@code WECHAT_MINIAPP_APP_ID}；默认空串。 */
    private String appId = "";
    /** 小程序密钥，对应 {@code wechat.miniapp.secret}，环境变量 {@code WECHAT_MINIAPP_SECRET}；默认空串，禁止入库或打印。 */
    private String secret = "";

    /** 两个值都非空白才算配置完成，微信接口据此决定正常调用还是直接 503。 */
    public boolean configured() {
        return !normalizedAppId().isEmpty() && !normalizedSecret().isEmpty();
    }

    /** 去除首尾空白后的 AppID；null 按空串处理，便于调用方直接判空。 */
    public String normalizedAppId() {
        return appId == null ? "" : appId.trim();
    }

    /** 去除首尾空白后的 Secret；null 按空串处理。 */
    public String normalizedSecret() {
        return secret == null ? "" : secret.trim();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
