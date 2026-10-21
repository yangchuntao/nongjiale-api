package com.suzhong.njl.service.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.MerchantApply;
import com.suzhong.njl.domain.entity.ShelfApplication;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.MerchantApplyMapper;
import com.suzhong.njl.mapper.ShelfApplicationMapper;
import com.suzhong.njl.web.admin.dto.QueueCountResponse;

import org.springframework.stereotype.Service;

/**
 * 运营工作台队列计数服务。
 *
 * <p>职责：一次给出三类待办队列的 {@code PENDING} 计数，供工作台统计卡展示。
 * 只读，不开事务；三条 {@code selectCount} 各自命中状态索引，代价都是常数级。
 *
 * <p>计数口径：只看当前状态列，不做时间窗过滤；与三个队列列表页的 {@code status=PENDING}
 * 筛选口径一致，保证卡片数字和点进去看到的 total 相同。
 */
@Service
public class AdminQueueCountService {

    private final BookingIntentMapper bookingIntentMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final ShelfApplicationMapper shelfApplicationMapper;

    public AdminQueueCountService(
            BookingIntentMapper bookingIntentMapper,
            MerchantApplyMapper merchantApplyMapper,
            ShelfApplicationMapper shelfApplicationMapper) {
        this.bookingIntentMapper = bookingIntentMapper;
        this.merchantApplyMapper = merchantApplyMapper;
        this.shelfApplicationMapper = shelfApplicationMapper;
    }

    /** 统计三类待办队列的 PENDING 数量。 */
    public QueueCountResponse pendingCounts() {
        return new QueueCountResponse(
                count(bookingIntentMapper.selectCount(Wrappers.<BookingIntent>lambdaQuery()
                        .eq(BookingIntent::getStatus, IntentStatus.PENDING))),
                count(merchantApplyMapper.selectCount(Wrappers.<MerchantApply>lambdaQuery()
                        .eq(MerchantApply::getStatus, ReviewStatus.PENDING))),
                count(shelfApplicationMapper.selectCount(Wrappers.<ShelfApplication>lambdaQuery()
                        .eq(ShelfApplication::getStatus, ReviewStatus.PENDING))));
    }

    /** selectCount 返回包装类型，null 按 0 处理。 */
    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
