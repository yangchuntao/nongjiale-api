package com.suzhong.njl.web.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ShelfAction;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商家上下架申请请求体（{@code POST /api/merchant/stores/{id}/shelf-applications}，须商家 Bearer）。
 *
 * <p>只能对本主体名下的门店提交，门店不存在或不属于本主体返回 404。同一门店同时只允许一条待审申请，
 * 已有待审时返回 409「已有待审的上下架申请」。申请本身不改变门店状态，须运营审批通过后才生效。
 * 未知字段会被拒绝。
 *
 * @param action 申请动作，必填，见 {@link com.suzhong.njl.domain.enums.ShelfAction}：
 *               {@code PUBLISH} 申请上架、{@code UNPUBLISH} 申请下架；其他值反序列化失败返回 400
 * @param reason 申请原因，可空，最长 500 字；供运营审批参考
 */
@Schema(name = "ShelfApplicationRequest", description = "商家上下架申请请求体；只能对本主体名下门店提交，申请本身不改变门店状态，须运营审批通过后生效。"
        + "同一门店同时只允许一条待审申请，重复提交返回 409「已有待审的上下架申请」")
@JsonIgnoreProperties(ignoreUnknown = false)
public record ShelfApplicationRequest(
        @Schema(description = "申请动作，必填：PUBLISH 申请上架、UNPUBLISH 申请下架（枚举名，非 ON/OFF）；其他值反序列化失败返回 400",
                allowableValues = {"PUBLISH", "UNPUBLISH"})
        @NotNull(message = "上下架动作不能为空")
        ShelfAction action,
        @Schema(description = "申请原因，可空，最长 500 字；供运营审批参考")
        @Size(max = 500, message = "申请原因过长")
        String reason) {
}
