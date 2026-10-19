package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.IntentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 意向状态变更请求体（{@code POST /api/admin/intents/{id}/status}）。
 *
 * <p>合法迁移：PENDING → CONFIRMED、RESCHEDULED、CLOSED；CONFIRMED → CHECKED_IN、CLOSED；
 * RESCHEDULED → CONFIRMED、CLOSED；CHECKED_IN 与 CLOSED 为终态。
 * 非法迁移或版本冲突返回 409，意向不存在返回 404，未知字段返回 400「请求体格式不正确」。
 * 成功后返回详情，并追加一条 {@code STATUS_CHANGE} 备注。
 *
 * @param targetStatus 目标状态，必填，取值见 {@link IntentStatus}；为空报「目标状态不能为空」，非法码按「请求体格式不正确」拒绝
 * @param note         变更说明，可空，最长 1000 字；空白串落库为 null
 * @param version      乐观锁版本号，必填；为空报「版本不能为空」，与库内不一致返回 409「数据已被修改」
 */
public record IntentStatusRequest(
        @NotNull(message = "目标状态不能为空")
        IntentStatus targetStatus,
        @Size(max = 1000, message = "备注过长")
        String note,
        @NotNull(message = "版本不能为空")
        Long version) {
}
