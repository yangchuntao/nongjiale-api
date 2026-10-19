package com.suzhong.njl.web.portal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商家入驻申请请求体（{@code POST /api/merchant-applies}，公开接口，无需登录）。
 *
 * <p>只落一条待审申请，不建账号、不建门店；审核与开通由运营端完成。允许重复提交，
 * 联系电话不做唯一性校验；地区只要求存在且 {@code enabled=true}，不要求该市已开通。
 * 未知字段会被拒绝。
 *
 * @param orgName         商家主体名称，必填，最长 128 字
 * @param contactName     联系人，可空，最长 64 字
 * @param contactPhone    联系电话，可空，最长 20 字；非空白时必须是 11 位手机号，否则 400
 * @param plannedStoreName 拟开门店名，可空，最长 128 字；仅作运营参考，不校验重名
 * @param regionId        意向地区 ID，可空；传了就必须存在且已启用，否则 400「地区不存在」
 * @param description     商家/门店介绍，可空，最长 1000 字
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(name = "MerchantApplyRequest", description = "商家入驻申请请求体，POST /api/merchant-applies，公开接口；只落一条待审申请，不建账号、不建门店")
public record MerchantApplyRequest(
        @Schema(description = "商家主体名称，必填，最长 128 字", example = "广陵表弟农家")
        @NotBlank(message = "请填写主体名称")
        @Size(max = 128, message = "主体名称过长")
        String orgName,
        @Schema(description = "联系人，可空，最长 64 字")
        @Size(max = 64, message = "联系人过长")
        String contactName,
        @Schema(description = "联系电话，可空，最长 20 字；非空白时必须是 11 位手机号，否则 400")
        @Size(max = 20, message = "联系电话过长")
        String contactPhone,
        @Schema(description = "拟开门店名，可空，最长 128 字；仅作运营参考，不校验重名")
        @Size(max = 128, message = "门店名过长")
        String plannedStoreName,
        @Schema(description = "意向地区 ID，可空；传了就必须存在且已启用，否则 400「地区不存在」")
        Long regionId,
        @Schema(description = "商家/门店介绍，可空，最长 1000 字")
        @Size(max = 1000, message = "介绍过长")
        String description) {
}
