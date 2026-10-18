package com.suzhong.njl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 小程序 AppID / Secret 只来自环境变量。空串视为未配置，不阻止进程启动。
 */
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatMiniappProperties {

    private String appId = "";
    private String secret = "";

    public boolean configured() {
        return !normalizedAppId().isEmpty() && !normalizedSecret().isEmpty();
    }

    public String normalizedAppId() {
        return appId == null ? "" : appId.trim();
    }

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
