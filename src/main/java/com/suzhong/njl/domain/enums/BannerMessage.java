package com.suzhong.njl.domain.enums;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 广告位接口的状态与中文句子。业务代码只抛这些常量，不另写一句中文。
 */
public enum BannerMessage {
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "请填写图片地址"),
    IMAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "图片地址过长"),
    IMAGE_SCHEME(HttpStatus.BAD_REQUEST, "图片地址须为 http 或 https"),
    LINK_REQUIRED(HttpStatus.BAD_REQUEST, "请填写跳转链接"),
    LINK_TOO_LONG(HttpStatus.BAD_REQUEST, "跳转链接过长"),
    LINK_INVALID(HttpStatus.BAD_REQUEST, "跳转链接须为站内路径或 http(s) 外链"),
    SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "请选择投放范围"),
    ALL_WITH_CITY(HttpStatus.BAD_REQUEST, "全站投放不能指定市"),
    CITY_REQUIRED(HttpStatus.BAD_REQUEST, "请选择已开通的市"),
    CITY_NOT_OPEN(HttpStatus.BAD_REQUEST, "投放市未开通，请改选全站或其他已开通市"),
    REGION_DISABLED(HttpStatus.BAD_REQUEST, "地区不可用"),
    SORT_INVALID(HttpStatus.BAD_REQUEST, "请输入整数排序值"),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "标题过长"),
    ENABLED_REQUIRED(HttpStatus.BAD_REQUEST, "请指定是否启用"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Banner 不存在"),
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND, "城市不存在"),
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

    public ApiException toException() {
        return new ApiException(status, text);
    }
}
