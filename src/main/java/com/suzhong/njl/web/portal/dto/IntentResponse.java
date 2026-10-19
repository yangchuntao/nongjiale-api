package com.suzhong.njl.web.portal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;

/**
 * 预订意向（{@code POST /api/booking-intents}、{@code GET /api/me/intents} 与
 * {@code GET /api/me/intents/{id}} 的响应体）。
 *
 * <p>门户端只能看到自己的意向；联系电话返回完整 11 位，不做脱敏（脱敏只发生在商家/运营的只读接口）。
 * 响应不带商家主体 ID 与版本号，避免暴露内部字段。
 *
 * @param id           意向 ID
 * @param intentNo     意向单号，由「日期 + 随机十六进制」生成，对客展示与查询均可用
 * @param status       跟进状态，见 {@link com.suzhong.njl.domain.enums.IntentStatus}；新提交固定 {@code PENDING}
 * @param storeId      门店 ID
 * @param storeName    门店展示名，可空；门店被物理删除后为 null，不影响意向可读
 * @param checkIn      到店日期，格式 {@code yyyy-MM-dd}
 * @param checkOut     离店日期，可空，格式 {@code yyyy-MM-dd}；只填晚数时为空
 * @param nights       入住晚数，可空；只填离店日期时为空
 * @param guests       入住人数
 * @param contactName  联系人姓名
 * @param contactPhone 联系电话，完整 11 位，不脱敏
 * @param userRemark   游客备注，可空
 * @param createdAt    创建时间，服务器本地时间（Asia/Shanghai）
 */
public record IntentResponse(
        Long id,
        String intentNo,
        IntentStatus status,
        Long storeId,
        String storeName,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guests,
        String contactName,
        String contactPhone,
        String userRemark,
        LocalDateTime createdAt) {
}
