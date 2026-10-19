package com.suzhong.njl.web.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运营端意向列表项（{@code GET /api/admin/intents} 的 items 元素）。
 *
 * <p>列表按 {@code updatedAt} 倒序、ID 倒序返回；手机号为完整号码，不做脱敏，
 * 与商家端「中间四位脱敏」的口径不同。时间字段为 {@code Asia/Shanghai} 的 ISO-8601。
 *
 * @param id            意向 ID，对应 {@code booking_intent.id}
 * @param intentNo      意向单号，门户生成
 * @param userId        提交意向的游客 ID
 * @param storeId       门店 ID
 * @param storeName     门店名称；批量查询门店后回填，门店不存在时为 null
 * @param merchantOrgId 门店所属商家主体 ID
 * @param checkIn       到店日期，{@code YYYY-MM-DD}
 * @param checkOut      离店日期，可空
 * @param nights        住店晚数，可空
 * @param guests        入住人数，非空
 * @param contactName   联系人姓名，非空
 * @param contactPhone  联系人手机号，完整号码
 * @param userRemark    游客备注，可空
 * @param status        状态码，取值见 {@link IntentStatus}；库内无法识别的码为 null
 * @param statusLabel   状态中文文案，取 {@link IntentStatus#label()}，无法识别时为空串
 * @param version       乐观锁版本号
 * @param createdAt     创建时间，{@code Asia/Shanghai} 的 ISO-8601
 * @param updatedAt     最后更新时间，格式同上
 */
@Schema(name = "AdminIntentItem", description = "运营端意向列表项响应，用于 GET /api/admin/intents 的 items 元素")
public record AdminIntentItem(
        @Schema(description = "意向 ID，对应 booking_intent.id")
        Long id,
        @Schema(description = "意向单号，门户生成")
        String intentNo,
        @Schema(description = "提交意向的游客 ID")
        Long userId,
        @Schema(description = "门店 ID")
        Long storeId,
        @Schema(description = "门店名称；门店不存在时为 null")
        String storeName,
        @Schema(description = "门店所属商家主体 ID")
        Long merchantOrgId,
        @Schema(description = "到店日期，格式 YYYY-MM-DD")
        LocalDate checkIn,
        @Schema(description = "离店日期，可空")
        LocalDate checkOut,
        @Schema(description = "住店晚数，可空")
        Integer nights,
        @Schema(description = "入住人数，非空")
        Integer guests,
        @Schema(description = "联系人姓名，非空")
        String contactName,
        @Schema(description = "联系人手机号，运营端返回完整号码，不做脱敏（与商家端脱敏口径不同）")
        String contactPhone,
        @Schema(description = "游客备注，可空")
        String userRemark,
        @Schema(description = "状态码，取值 PENDING、CONFIRMED、RESCHEDULED、CHECKED_IN、CLOSED；库内无法识别的码为 null")
        IntentStatus status,
        @Schema(description = "状态中文文案，无法识别时为空串")
        String statusLabel,
        @Schema(description = "乐观锁版本号")
        Long version,
        @Schema(description = "创建时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime createdAt,
        @Schema(description = "最后更新时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime updatedAt) {
}
