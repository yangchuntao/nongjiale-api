package com.suzhong.njl.wechat;

/**
 * 微信小程序服务端能力的抽象，业务层只依赖本接口。
 *
 * <p>职责：把微信协议细节（URL、参数、access_token、errcode）挡在实现类内部，
 * 对外只暴露「code 换 openid」「手机号 code 换号码」两个业务语义。
 *
 * <p>关键限制：实现必须是线程安全的单例；所有失败都以
 * {@link com.suzhong.njl.web.ApiException}（状态码来自 {@code WechatMessage}）抛出，
 * 不返回 {@code null}。测试可提供桩实现，避免真实出站。
 */
public interface WechatMiniappClient {

    /**
     * 用 {@code wx.login} 的一次性 code 换取 openid。
     *
     * @param jsCode 微信登录 code，不能为空；同一 code 只能成功使用一次
     * @return 会话标识；openid 必非空，unionid 未返回时为 {@code null}
     * @throws com.suzhong.njl.web.ApiException 未配置 503、code 失效 401、上游异常 502
     */
    JsCodeSession codeToSession(String jsCode);

    /**
     * 用手机号组件的一次性 code 换取纯手机号。
     *
     * @param phoneCode 手机号组件返回的 code，不能为空且只能用一次
     * @return 大陆 11 位纯手机号
     * @throws com.suzhong.njl.web.ApiException 未配置 503、号码非法 400、code 失效 401、上游异常 502
     */
    PurePhoneNumber phoneNumber(String phoneCode);

    /**
     * jscode2session 的结果。
     *
     * @param openid  用户在当前小程序 AppID 下的唯一标识，非空
     * @param unionid 同一微信开放平台账号下的统一标识；未绑定开放平台或未返回时为 {@code null}
     */
    record JsCodeSession(String openid, String unionid) {
    }

    /**
     * 微信手机号组件返回的号码。
     *
     * @param purePhoneNumber 不含国家码与分隔符的大陆 11 位手机号
     */
    record PurePhoneNumber(String purePhoneNumber) {
    }
}
