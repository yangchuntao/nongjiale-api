package com.suzhong.njl.domain.enums;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 微信小程序接口的状态与中文句子。业务代码只抛这些常量。
 */
public enum WechatMessage {
    CODE_REQUIRED(HttpStatus.BAD_REQUEST, "请提供微信登录凭证"),
    BODY_INVALID(HttpStatus.BAD_REQUEST, "请求体格式不正确"),
    PHONE_INVALID(HttpStatus.BAD_REQUEST, "手机号格式不正确"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "请设置网页版登录密码"),
    PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "密码长度为 6 到 64 位"),
    NEED_WECHAT_LOGIN(HttpStatus.BAD_REQUEST, "请先使用微信登录"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未登录"),
    CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "微信登录已失效，请重试"),
    BAD_PASSWORD(HttpStatus.UNAUTHORIZED, "手机号或密码错误"),
    PHONE_REQUIRED_FOR_INTENT(HttpStatus.FORBIDDEN, "请先绑定手机号，完成与网页版账号打通后再提交意向"),
    ALREADY_OTHER_PHONE(HttpStatus.CONFLICT, "当前微信已绑定其他手机号"),
    PHONE_BOUND_OTHER_WECHAT(HttpStatus.CONFLICT, "该手机号已绑定其他微信"),
    SHELL_HAS_INTENTS(HttpStatus.CONFLICT, "未打通账号下已有意向，不能合并"),
    UNAVAILABLE(HttpStatus.BAD_GATEWAY, "微信服务暂不可用"),
    NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "微信登录未配置");

    /** 未打通壳用户的展示名。服务里不要再写这四个字。 */
    public static final String SHELL_DISPLAY_NAME = "微信用户";

    private final HttpStatus status;
    private final String text;

    WechatMessage(HttpStatus status, String text) {
        this.status = status;
        this.text = text;
    }

    public HttpStatus status() {
        return status;
    }

    public String text() {
        return text;
    }

    public ApiException toException() {
        return new ApiException(status, text);
    }
}
