package com.suzhong.njl.web.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;

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
public record AdminIntentItem(
        Long id,
        String intentNo,
        Long userId,
        Long storeId,
        String storeName,
        Long merchantOrgId,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guests,
        String contactName,
        String contactPhone,
        String userRemark,
        IntentStatus status,
        String statusLabel,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
