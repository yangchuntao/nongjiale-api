package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 站点配置保存请求体（{@code PUT /api/admin/site-config}）。
 *
 * <p>三项都是必填，落库前去除首尾空白，写入后失效 {@code njl:site-config:public} 缓存。
 * 当前实现只接收这三项：品牌英文名、副标、Logo、图标等目标契约字段尚未实现，
 * 多传会被拒绝并返回 400「请求体格式不正确」。不修改开城阈值配置。
 *
 * @param siteName     站名，必填，最长 64 字；为空报「站名不能为空」，超长报「站名过长」
 * @param brandColor   主色，必填，须匹配 {@code ^#[0-9A-Fa-f]{6}$}；为空报「主色不能为空」，格式不符报「主色格式不正确」
 * @param contactPhone 客服电话，必填，最长 32 字；为空报「客服电话不能为空」，超长报「客服电话过长」
 */
public record SiteConfigWriteRequest(
        @NotBlank(message = "站名不能为空")
        @Size(max = 64, message = "站名过长")
        String siteName,
        @NotBlank(message = "主色不能为空")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "主色格式不正确")
        String brandColor,
        @NotBlank(message = "客服电话不能为空")
        @Size(max = 32, message = "客服电话过长")
        String contactPhone) {
}
