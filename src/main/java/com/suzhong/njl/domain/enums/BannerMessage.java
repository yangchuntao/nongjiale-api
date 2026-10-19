package com.suzhong.njl.domain.enums;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 广告位接口的状态与中文句子。业务代码只抛这些常量，不另写一句中文。
 *
 * <p>不落库：没有 {@code @EnumValue}，每个常量自带 HTTP 状态码与面向运营的提示文案，
 * 由 {@link #toException()} 转成统一信封的 {@code ApiException}。
 * 新增校验时优先复用已有常量，避免同一错误出现两种说法。
 */
public enum BannerMessage {
    /** 图片地址为空。 */
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "请填写图片地址"),
    /** 图片地址超过 512 字符上限。 */
    IMAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "图片地址过长"),
    /** 图片地址不是 http 或 https 协议。 */
    IMAGE_SCHEME(HttpStatus.BAD_REQUEST, "图片地址须为 http 或 https"),
    /** 跳转链接为空。 */
    LINK_REQUIRED(HttpStatus.BAD_REQUEST, "请填写跳转链接"),
    /** 跳转链接超过 512 字符上限。 */
    LINK_TOO_LONG(HttpStatus.BAD_REQUEST, "跳转链接过长"),
    /** 跳转链接既不是站内路径也不是 http(s) 外链。 */
    LINK_INVALID(HttpStatus.BAD_REQUEST, "跳转链接须为站内路径或 http(s) 外链"),
    /** 未指定投放范围 {@code ALL} 或 {@code CITY}。 */
    SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "请选择投放范围"),
    /** 选了全站投放却又传了城市 ID，两者互斥。 */
    ALL_WITH_CITY(HttpStatus.BAD_REQUEST, "全站投放不能指定市"),
    /** 按市投放但未指定城市。 */
    CITY_REQUIRED(HttpStatus.BAD_REQUEST, "请选择已开通的市"),
    /** 目标城市当前未开通，不允许按市投放。 */
    CITY_NOT_OPEN(HttpStatus.BAD_REQUEST, "投放市未开通，请改选全站或其他已开通市"),
    /** 目标地区已停用，不能作为投放城市。 */
    REGION_DISABLED(HttpStatus.BAD_REQUEST, "地区不可用"),
    /** 排序值不是整数，或超出 0 至 1000000 的允许范围。 */
    SORT_INVALID(HttpStatus.BAD_REQUEST, "请输入整数排序值"),
    /** 标题超过 64 字符上限。 */
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "标题过长"),
    /** 未指定启用状态。 */
    ENABLED_REQUIRED(HttpStatus.BAD_REQUEST, "请指定是否启用"),
    /** Banner 记录不存在。 */
    NOT_FOUND(HttpStatus.NOT_FOUND, "Banner 不存在"),
    /** 城市记录不存在。 */
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND, "城市不存在"),
    /** 启用时发现投放市已关城，与停用/修改不同，这里是 409 冲突。 */
    ENABLE_CITY_CLOSED(HttpStatus.CONFLICT, "投放市未开通，请改选全站或其他已开通市");

    private final HttpStatus status;
    private final String text;

    BannerMessage(HttpStatus status, String text) {
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
