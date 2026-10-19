package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 站点配置（{@code GET /api/admin/site-config} 与 {@code PUT /api/admin/site-config} 的 data）。
 *
 * <p>当前只读写站名、主色、客服电话三项，值来自 {@code site_config} 配置表；
 * 键不存在或值为 null 时统一返回空串，前端可据此走兜底展示。
 * 品牌英文名、副标、Logo、图标等字段尚未实现，不在本结构中。
 *
 * @param siteName     站名，对应配置键 {@code site.name}
 * @param brandColor   站点主色，{@code #RRGGBB} 形式，对应 {@code site.brand_color}
 * @param contactPhone 客服电话，展示用字符串，对应 {@code site.contact_phone}
 */
@Schema(name = "SiteConfigResponse", description = "站点配置响应，用于 GET /api/admin/site-config 与 PUT /api/admin/site-config 的 data")
public record SiteConfigResponse(
        @Schema(description = "站名，对应配置键 site.name；键不存在或值为 null 时返回空串")
        String siteName,
        @Schema(description = "站点主色，#RRGGBB 形式，对应配置键 site.brand_color；缺失时返回空串")
        String brandColor,
        @Schema(description = "客服电话，展示用字符串，对应配置键 site.contact_phone；缺失时返回空串")
        String contactPhone) {
}
