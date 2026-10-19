package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.SiteConfig;
/**
 * 站点配置 Mapper，对应 {@code site_config} 表。
 *
 * <p>无自定义 SQL。站名、主色、客服电话等展示配置通常只有一行；读取结果走旁路缓存，
 * 运营保存后由 Service 主动失效。
 */
public interface SiteConfigMapper extends BaseMapper<SiteConfig> {
}
