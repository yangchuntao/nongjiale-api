package com.suzhong.njl.web.admin.dto;

/**
 * 运营工作台待办队列计数快照。
 *
 * <p>三个数字分别是待跟进意向、待审入驻申请、待审上下架申请的数量，
 * 供工作台统计卡一次请求拿全，替代旧的「拉三个分页列表只取 total」。
 *
 * @param intents            待跟进（{@code PENDING}）意向数
 * @param merchantApplies    待审（{@code PENDING}）入驻申请数
 * @param shelfApplications  待审（{@code PENDING}）上下架申请数
 */
public record QueueCountResponse(
        long intents,
        long merchantApplies,
        long shelfApplications) {
}
