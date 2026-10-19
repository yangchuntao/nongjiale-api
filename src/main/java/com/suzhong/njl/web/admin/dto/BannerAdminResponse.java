package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.BannerScope;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运营端首页 Banner（{@code GET /api/admin/banners} 的 items 元素、详情、
 * 创建/更新/启停接口的 data）。
 *
 * <p>{@code citySlug}、{@code cityName}、{@code cityOpen} 由服务端按 {@code cityId} 实时补查，
 * 不是库内快照：市被关城后 {@code cityOpen} 立即变为 false，但 Banner 的 {@code enabled} 不会被自动改写。
 * 时间字段为 {@code Asia/Shanghai} 的 ISO-8601。
 *
 * @param id         Banner ID
 * @param title      运营标题，可空；空白串在落库前转为 null
 * @param imageUrl   图片地址，非空，最长 512，必须是 http:// 或 https://
 * @param linkUrl    跳转地址，非空，最长 512；站内以单个 / 开头，或 http(s) 外链
 * @param scope      投放范围，取值见 {@link BannerScope}：ALL（全站）、CITY（指定市）
 * @param cityId     投放市 ID；ALL 时为 null
 * @param citySlug   投放市 slug，如 yangzhou；非 CITY、市不存在时为 null
 * @param cityName   投放市名称，如扬州市；非 CITY、市不存在时为 null
 * @param cityOpen   CITY 时表示该市当前是否开通；ALL 时为 null
 * @param sortOrder  排序值，0 到 1000000，越小越靠前；同值按 ID 升序
 * @param enabled    是否启用；关闭后门户不展示
 * @param createdAt  创建时间
 * @param updatedAt  最后更新时间
 */
@Schema(name = "BannerAdminResponse", description = "运营端首页 Banner 响应，用于 GET /api/admin/banners 的 items 元素、详情及创建/更新/启停接口的 data")
public record BannerAdminResponse(
        @Schema(description = "Banner ID")
        Long id,
        @Schema(description = "运营标题，可空；空白串在落库前转为 null")
        String title,
        @Schema(description = "图片地址，非空，最长 512，必须是 http:// 或 https://")
        String imageUrl,
        @Schema(description = "跳转地址，非空，最长 512；站内以单个 / 开头，或 http(s) 外链")
        String linkUrl,
        @Schema(description = "投放范围，取值 ALL（全站）、CITY（指定市）")
        BannerScope scope,
        @Schema(description = "投放市 ID；ALL 时为 null")
        Long cityId,
        @Schema(description = "投放市 slug，如 yangzhou；非 CITY、市不存在时为 null")
        String citySlug,
        @Schema(description = "投放市名称，如扬州市；非 CITY、市不存在时为 null")
        String cityName,
        @Schema(description = "CITY 时表示该市当前是否开通；ALL 时为 null")
        Boolean cityOpen,
        @Schema(description = "排序值，0 到 1000000，越小越靠前；同值按 ID 升序")
        Integer sortOrder,
        @Schema(description = "是否启用；关闭后门户不展示")
        Boolean enabled,
        @Schema(description = "创建时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime createdAt,
        @Schema(description = "最后更新时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime updatedAt) {
}
