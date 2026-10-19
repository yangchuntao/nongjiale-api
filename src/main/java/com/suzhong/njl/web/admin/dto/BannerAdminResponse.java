package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.BannerScope;

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
public record BannerAdminResponse(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        BannerScope scope,
        Long cityId,
        String citySlug,
        String cityName,
        Boolean cityOpen,
        Integer sortOrder,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
