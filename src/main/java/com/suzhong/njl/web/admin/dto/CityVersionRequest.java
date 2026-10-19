package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 开城/关城请求体（{@code POST /api/admin/cities/{cityId}/open}、
 * {@code POST /api/admin/cities/{cityId}/close}）。
 *
 * <p>只带乐观锁版本号；没有清单行时必须是 0。版本不一致返回 409「数据已被修改」，
 * 已开通再开通或未开通再关城分别返回 409「该市已开通」「该市未开通」。
 *
 * @param version 清单版本号，必填；为空报「版本不能为空」
 */
@Schema(name = "CityVersionRequest", description = "开城/关城请求体，用于 POST /api/admin/cities/{cityId}/open 与 POST /api/admin/cities/{cityId}/close")
public record CityVersionRequest(
        @Schema(description = "清单版本号，必填；没有清单行时必须是 0，为空报「版本不能为空」，与库内不一致返回 409「数据已被修改」")
        @NotNull(message = "版本不能为空") Long version) {
}
