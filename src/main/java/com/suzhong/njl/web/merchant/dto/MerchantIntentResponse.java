package com.suzhong.njl.web.merchant.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.IntentStatus;

/**
 * 商家端预订意向（{@code GET /api/merchant/intents} 的 items 元素与
 * {@code GET /api/merchant/intents/{id}} 的响应体）。
 *
 * <p>只读，且只返回本商家主体名下的意向。联系电话中间四位脱敏（前 3 位 + 4 个星号 + 后 4 位），
 * 完整号码只允许运营端查看；意向不存在或不属于本主体统一返回 404，避免探测他人单号。
 *
 * @param id           意向 ID
 * @param intentNo     意向单号，由「日期 + 随机十六进制」生成
 * @param status       跟进状态，见 {@link com.suzhong.njl.domain.enums.IntentStatus}；只能由运营推进
 * @param storeId      门店 ID
 * @param storeName    门店展示名，可空；门店被物理删除后为 null
 * @param checkIn      到店日期，格式 {@code yyyy-MM-dd}
 * @param checkOut     离店日期，可空；游客只填晚数时为空
 * @param nights       入住晚数，可空；游客只填离店日期时为空
 * @param guests       入住人数
 * @param contactName  联系人姓名
 * @param contactPhone 联系电话，已脱敏为 {@code 138****1234}；非 11 位数字的脏数据按通用规则脱敏
 * @param userRemark   游客备注，可空
 * @param createdAt    创建时间，服务器本地时间（Asia/Shanghai）
 */
public record MerchantIntentResponse(
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
