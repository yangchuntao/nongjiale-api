package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record WechatLoginRequest(String code) {
}
