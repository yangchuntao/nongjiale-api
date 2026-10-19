package com.suzhong.njl.web.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开站点配置（{@code GET /api/config/public} 的响应体，无需登录）。
 *
 * <p>当前只返回站名、主色、客服电话三项；品牌站点配置（英文站名、副标、Logo、图标）的目标契约
 * 见 {@code docs/TECH-DESIGN-品牌站点配置.md}，尚未在本结构落地。客服电话不随门店下架或关城变化。
 * 未配置的项一律返回空串而不是 null，前端按「未配置」兜底。
 *
 * @param siteName     中文站名，未配置时为空串
 * @param brandColor   主题主色，十六进制颜色值（如 {@code #4BA3D9}），未配置时为空串
 * @param contactPhone 客服电话，未配置时为空串
 */
@Schema(name = "PublicConfigResponse", description = "公开站点配置，GET /api/config/public 的响应体，无需登录；未配置的项一律返回空串而不是 null")
public record PublicConfigResponse(
        @Schema(description = "中文站名，未配置时为空串")
        String siteName,
        @Schema(description = "主题主色，十六进制颜色值（如 #4BA3D9），未配置时为空串", example = "#4BA3D9")
        String brandColor,
        @Schema(description = "客服电话，未配置时为空串；不随门店下架或关城变化", example = "400-000-0000")
        String contactPhone) {
}
