package com.suzhong.njl.wechat;

public interface WechatMiniappClient {

    JsCodeSession codeToSession(String jsCode);

    PurePhoneNumber phoneNumber(String phoneCode);

    record JsCodeSession(String openid, String unionid) {
    }

    record PurePhoneNumber(String purePhoneNumber) {
    }
}
