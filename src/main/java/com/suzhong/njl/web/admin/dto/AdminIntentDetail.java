package com.suzhong.njl.web.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.IntentStatus;

/**
 * 运营端意向详情（{@code GET /api/admin/intents/{id}} 的 data）。
 *
 * <p>状态变更 {@code POST /api/admin/intents/{id}/status} 与追加备注
 * {@code POST /api/admin/intents/{id}/notes} 也返回本结构，便于前端一次刷新详情与时间线。
 * 与列表项相比只多 {@code notes}；手机号是完整号码，运营端不做脱敏。
 * 时间字段按 {@code Asia/Shanghai} 的 ISO-8601 输出，日期字段为 {@code YYYY-MM-DD}。
 *
 * @param id            意向 ID，对应 {@code booking_intent.id}
 * @param intentNo      意向单号，门户生成，用于对客沟通与导出
 * @param userId        提交意向的游客 ID，对应 {@code app_user.id}
 * @param storeId       门店 ID
 * @param storeName     门店名称；门店查不到时为 null
 * @param merchantOrgId 门店所属商家主体 ID
 * @param checkIn       到店日期，{@code YYYY-MM-DD}
 * @param checkOut      离店日期，可空，表示未填
 * @param nights        住店晚数，可空，由门户按到店/离店日期写入
 * @param guests        入住人数，非空
 * @param contactName   联系人姓名，非空
 * @param contactPhone  联系人手机号，完整号码，不脱敏
 * @param userRemark    游客备注，可空
 * @param status        状态码，取值见 {@link IntentStatus}：PENDING、CONFIRMED、RESCHEDULED、CHECKED_IN、CLOSED
 * @param statusLabel   状态中文文案，取 {@link IntentStatus#label()}；库内状态码无法识别时为空串
 * @param version       乐观锁版本号，状态变更时必须原样回传
 * @param createdAt     创建时间，{@code Asia/Shanghai} 的 ISO-8601
 * @param updatedAt     最后更新时间，格式同上
 * @param notes         跟进记录，按 ID 从早到晚；无记录时为空列表
 */
public record AdminIntentDetail(
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
        LocalDateTime updatedAt,
        List<IntentNoteItem> notes) {
}
