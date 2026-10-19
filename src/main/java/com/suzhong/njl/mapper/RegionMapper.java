package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.Region;
/**
 * 地区 Mapper，对应 {@code region} 表。
 *
 * <p>无自定义 SQL。地区分省/市/区县三级，门户只暴露已开通的市及其下仍启用的区县；
 * 层级与启用状态由 Service 与城市可见性组件解释，SQL 层不做联表。
 */
public interface RegionMapper extends BaseMapper<Region> {
}
