package com.suzhong.njl.web.portal.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 提交预订意向的请求体（{@code POST /api/booking-intents}，须游客 Bearer）。
 *
 * <p>校验分两层：本 record 上的注解负责格式与必填（失败 400）；日期先后、晚数与离店日期至少给一个、
 * 账号是否已绑定手机号由 {@code BookingIntentService} 判断（403/409）。
 * {@code portal_user.phone} 为空时提交直接返回 403「请先绑定手机号，完成与网页版账号打通后再提交意向」，
 * 且在插入之前抛出，不落库。未知字段会被拒绝，前端多传字段同样返回 400。
 *
 * @param storeId      门店 ID，必填；门店不存在 404，已下架或所属市未开通 410
 * @param checkIn      到店日期，必填，格式 {@code yyyy-MM-dd}；早于今天（Asia/Shanghai）返回 409
 * @param checkOut     离店日期，可空，格式 {@code yyyy-MM-dd}；与 {@code nights} 至少填一个，早于到店日期返回 409
 * @param nights       入住晚数，可空，整数且不小于 1；与 {@code checkOut} 至少填一个，两者都为空返回 400
 * @param guests       入住人数，必填，整数且不小于 1
 * @param contactName  联系人姓名，必填，最长 64 字
 * @param contactPhone 联系电话，必填，须匹配 {@code ^1\d{10}$}（11 位手机号）
 * @param userRemark   游客备注，可空，最长 500 字
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateIntentRequest(
        @NotNull(message = "请选择房源")
        Long storeId,
        @NotNull(message = "请填写到店日期")
        LocalDate checkIn,
        LocalDate checkOut,
        @Min(value = 1, message = "晚数不正确")
        Integer nights,
        @NotNull(message = "请填写人数")
        @Min(value = 1, message = "人数不正确")
        Integer guests,
        @NotBlank(message = "请填写联系人")
        @Size(max = 64, message = "联系人过长")
        String contactName,
        @NotBlank(message = "请填写联系电话")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
        String contactPhone,
        @Size(max = 500, message = "备注过长")
        String userRemark) {
}
