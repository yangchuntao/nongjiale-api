package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.HomeBanner;

/**
 * 首页 Banner Mapper，对应 {@code home_banner} 表。
 *
 * <p>无自定义 SQL。只展示启用条目：全站条目始终在内，指定城市的条目仅在该市仍开通时出现；
 * 排序与筛选在 Service 中完成，该接口不进首页 feed 缓存。
 */
public interface HomeBannerMapper extends BaseMapper<HomeBanner> {
}
