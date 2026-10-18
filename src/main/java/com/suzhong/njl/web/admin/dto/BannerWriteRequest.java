package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.suzhong.njl.domain.enums.BannerScope;

@JsonIgnoreProperties(ignoreUnknown = false)
public record BannerWriteRequest(
        String title,
        String imageUrl,
        String linkUrl,
        BannerScope scope,
        Long cityId,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer sortOrder,
        Boolean enabled) {
}
