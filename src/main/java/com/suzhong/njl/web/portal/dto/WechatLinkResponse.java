package com.suzhong.njl.web.portal.dto;

public record WechatLinkResponse(
        Long userId,
        String phone,
        boolean phoneBound,
        boolean wechatBound,
        boolean accountLinked) {
}
