package com.suzhong.njl.domain.enums;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 微信小程序接口的状态与中文句子。业务代码只抛这些常量。
 *
 * <p>不落库：没有 {@code @EnumValue}，每个常量固定 HTTP 状态码与面向游客的提示，
 * 由 {@link #toException()} 转成统一信封。微信侧网络或凭证失败用 502 或 503，
 * 账号绑定冲突用 409，未登录与凭证失效用 401。
 */
public enum WechatMessage {
    /** 未传 {@code wx.login} 的 code。 */
    CODE_REQUIRED(HttpStatus.BAD_REQUEST, "请提供微信登录凭证"),
    /** 请求体缺失或 JSON 结构不合法。 */
    BODY_INVALID(HttpStatus.BAD_REQUEST, "请求体格式不正确"),
    /** 绑定手机号时手填号码不符合手机号格式。 */
    PHONE_INVALID(HttpStatus.BAD_REQUEST, "手机号格式不正确"),
    /** 首次打通账号时未设置网页版密码。 */
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "请设置网页版登录密码"),
    /** 密码长度不在 6 至 64 位之间。 */
    PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "密码长度为 6 到 64 位"),
    /** 绑定/解绑等操作要求当前令牌来自微信登录。 */
    NEED_WECHAT_LOGIN(HttpStatus.BAD_REQUEST, "请先使用微信登录"),
    /** 未携带或携带了无效的 Bearer 令牌。 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未登录"),
    /** code 已被使用或超过微信侧有效期；当前代码未引用，预留给失效场景。 */
    CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "微信登录已失效，请重试"),
    /** 手机号或密码错误，不区分是哪一项错。 */
    BAD_PASSWORD(HttpStatus.UNAUTHORIZED, "手机号或密码错误"),
    /** 游客未绑定手机号就提交预订意向，直接拒绝且不插入数据。 */
    PHONE_REQUIRED_FOR_INTENT(HttpStatus.FORBIDDEN, "请先绑定手机号，完成与网页版账号打通后再提交意向"),
    /** 当前微信已绑定另一个手机号，需先解绑。 */
    ALREADY_OTHER_PHONE(HttpStatus.CONFLICT, "当前微信已绑定其他手机号"),
    /** 目标手机号已被另一个微信占用，禁止重复绑定。 */
    PHONE_BOUND_OTHER_WECHAT(HttpStatus.CONFLICT, "该手机号已绑定其他微信"),
    /** 待合并的壳账号下已有意向，为避免归属错乱不允许自动合并。 */
    SHELL_HAS_INTENTS(HttpStatus.CONFLICT, "未打通账号下已有意向，不能合并"),
    /** 微信接口返回异常或网络失败，属于上游故障。 */
    UNAVAILABLE(HttpStatus.BAD_GATEWAY, "微信服务暂不可用"),
    /** 未配置 AppID 或 Secret，微信相关能力整体不可用。 */
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

    /** 把常量转成带 HTTP 状态的业务异常，由全局异常处理统一包成信封。 */
    public ApiException toException() {
        return new ApiException(status, text);
    }
}
