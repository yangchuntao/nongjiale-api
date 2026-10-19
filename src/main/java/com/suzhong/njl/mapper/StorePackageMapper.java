package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.StorePackage;
/**
 * 门店套餐 Mapper，对应 {@code store_package} 表。
 *
 * <p>无自定义 SQL。套餐允许没有行；门户详情以 {@code store.package_text} 为准，
 * 两处不一致时同样以文本字段为准，本表仅作结构化补充。
 */
public interface StorePackageMapper extends BaseMapper<StorePackage> {
}
