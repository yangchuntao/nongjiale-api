package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.ShelfApplication;
/**
 * 上下架申请 Mapper，对应 {@code shelf_application} 表。
 *
 * <p>无自定义 SQL。「同一门店同时只能有一条待审申请」由 Service 先查后写保证，
 * 库里没有对应唯一约束，因此不要绕过 Service 直接插入。
 */
public interface ShelfApplicationMapper extends BaseMapper<ShelfApplication> {
}
