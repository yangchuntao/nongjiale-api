package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.BookingIntent;
/**
 * 预订意向 Mapper，对应 {@code booking_intent} 表。
 *
 * <p>无自定义 SQL。实体带 {@code @Version} 乐观锁，运营改状态时按 id + version 更新，
 * 影响行数为 0 即视为并发冲突并返回 409；游客只能查自己的意向，条件由 Service 拼装。
 */
public interface BookingIntentMapper extends BaseMapper<BookingIntent> {
}
