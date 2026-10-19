package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.StoreTagRel;
/**
 * 门店标签关联 Mapper，对应 {@code store_tag_rel} 表。
 *
 * <p>无自定义 SQL，维护门店与标签的多对多关系。门户按标签筛选时由 {@code StoreMapper.xml}
 * 用 EXISTS 反查本表，不经过该接口，避免先查关联 ID 再回表。
 */
public interface StoreTagRelMapper extends BaseMapper<StoreTagRel> {
}
