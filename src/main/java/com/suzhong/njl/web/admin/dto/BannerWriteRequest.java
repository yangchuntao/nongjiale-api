package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.suzhong.njl.domain.enums.BannerScope;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 首页 Banner 新建与全量更新请求体（{@code POST /api/admin/banners}、
 * {@code PUT /api/admin/banners/{id}}）。
 *
 * <p>本类不加 Bean Validation 注解，字段校验集中在 {@code BannerRules}，按固定顺序命中即返回 400：
 * 图片为空/超长/协议不对 → 链接为空/超长/形态不对 → 标题超长 → 排序非法 → 范围为空 →
 * 全站却带市或指定市却没选。城市不存在返回 404，市节点停用或未开通返回 400。
 * 更新为全量覆盖：{@code sortOrder}、{@code enabled} 不传返回 400，{@code title}、{@code cityId} 可空。
 *
 * @param title     运营标题，可空；空白视同未填并存 null，最长 64 字，超长报「标题过长」
 * @param imageUrl  图片地址，必填，最长 512 字；必须 http:// 或 https://，空报「请填写图片地址」
 * @param linkUrl   跳转地址，必填，最长 512 字；站内须以单个 / 开头，或 http(s) 外链，空报「请填写跳转链接」
 * @param scope     投放范围，必填，取值见 {@link BannerScope}：ALL（全站）、CITY（指定市）；为空报「请选择投放范围」
 * @param cityId    投放市 ID，CITY 时必填且须为已开通的市，ALL 时必须为空；全站带市报「全站投放不能指定市」
 * @param sortOrder 排序值，新建可空（默认 100）、更新必填；0 到 1000000，越小越靠前；小数或字符串按「请求体格式不正确」拒绝，越界或缺失报「请输入整数排序值」
 * @param enabled   是否启用，新建可空（默认 true）、更新必填；为空报「请指定是否启用」
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(name = "BannerWriteRequest", description = "首页 Banner 新建与全量更新请求体，用于 POST /api/admin/banners 与 PUT /api/admin/banners/{id}；字段校验集中在 BannerRules")
public record BannerWriteRequest(
        @Schema(description = "运营标题，可空；空白视同未填并存 null，最长 64 字，超长报「标题过长」")
        String title,
        @Schema(description = "图片地址，必填，最长 512 字；必须 http:// 或 https://，空报「请填写图片地址」")
        String imageUrl,
        @Schema(description = "跳转地址，必填，最长 512 字；站内须以单个 / 开头，或 http(s) 外链，空报「请填写跳转链接」")
        String linkUrl,
        @Schema(description = "投放范围，必填，取值 ALL（全站）、CITY（指定市）；为空报「请选择投放范围」")
        BannerScope scope,
        @Schema(description = "投放市 ID，CITY 时必填且须为已开通的市，ALL 时必须为空；全站带市报「全站投放不能指定市」")
        Long cityId,
        @Schema(description = "排序值，新建可空（默认 100）、更新必填；0 到 1000000，越小越靠前；小数或字符串按「请求体格式不正确」拒绝")
        @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer sortOrder,
        @Schema(description = "是否启用，新建可空（默认 true）、更新必填；为空报「请指定是否启用」")
        Boolean enabled) {
}
