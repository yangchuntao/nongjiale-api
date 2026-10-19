package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.BannerScope;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 首页 Banner 条目（{@code GET /api/home/banners} 的响应数组元素，公开接口）。
 *
 * <p>只返回启用中的条目：全站条目始终可见，指定市条目仅在该市当前仍开通时出现；
 * 按 {@code sortOrder} 升序，数字越小越靠前。接口不进首页 feed 缓存，运营启停后立即生效。
 *
 * @param id         Banner ID
 * @param title      标题，可空
 * @param imageUrl   图片 URL，必填，只接受外链地址，不是文件上传结果
 * @param linkUrl    点击跳转 URL，必填；可以是站内路径（如 {@code /?city=yangzhou}）或外链
 * @param scope      投放范围，见 {@link com.suzhong.njl.domain.enums.BannerScope}：{@code ALL} 全站、{@code CITY} 指定市
 * @param cityId     投放市 ID，仅 {@code scope=CITY} 时有值，全站条目为 null
 * @param citySlug   投放市 slug，仅 {@code scope=CITY} 时有值，全站条目为 null
 * @param cityName   投放市展示名，仅 {@code scope=CITY} 时有值，全站条目为 null
 * @param sortOrder  排序号，必填且非空，升序；数字越小越靠前
 * @param enabled    是否启用；本接口只会返回 {@code true}，字段保留给前端做兼容
 */
@Schema(name = "HomeBannerItem", description = "首页 Banner 条目，GET /api/home/banners 的响应数组元素；只返回启用中的条目，按 sortOrder 升序")
public record HomeBannerItem(
        @Schema(description = "Banner ID")
        Long id,
        @Schema(description = "标题，可空")
        String title,
        @Schema(description = "图片 URL，必填，只接受外链地址，不是文件上传结果")
        String imageUrl,
        @Schema(description = "点击跳转 URL，必填；可以是站内路径（如 /?city=yangzhou）或外链")
        String linkUrl,
        @Schema(description = "投放范围：ALL 全站、CITY 指定市")
        BannerScope scope,
        @Schema(description = "投放市 ID，仅 scope=CITY 时有值，全站条目为 null")
        Long cityId,
        @Schema(description = "投放市 slug，仅 scope=CITY 时有值，全站条目为 null", example = "yangzhou")
        String citySlug,
        @Schema(description = "投放市展示名，仅 scope=CITY 时有值，全站条目为 null", example = "扬州市")
        String cityName,
        @Schema(description = "排序号，必填且非空，升序；数字越小越靠前")
        Integer sortOrder,
        @Schema(description = "是否启用；本接口只会返回 true，字段保留给前端做兼容")
        Boolean enabled) {
}
