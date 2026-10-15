package com.suzhong.njl.service.portal;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.web.portal.dto.PublicConfigResponse;

@Service
public class PublicConfigService {

    private static final TypeReference<PublicConfigResponse> TYPE = new TypeReference<>() {
    };

    private final SiteConfigMapper siteConfigMapper;
    private final CacheAside cacheAside;

    public PublicConfigService(SiteConfigMapper siteConfigMapper, CacheAside cacheAside) {
        this.siteConfigMapper = siteConfigMapper;
        this.cacheAside = cacheAside;
    }

    public PublicConfigResponse publicConfig() {
        return cacheAside.get(CacheName.SITE_CONFIG, "public", TYPE, this::load);
    }

    private PublicConfigResponse load() {
        return new PublicConfigResponse(
                value(SiteConfigKey.NAME),
                value(SiteConfigKey.BRAND_COLOR),
                value(SiteConfigKey.CONTACT_PHONE));
    }

    private String value(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null || row.getConfigValue() == null ? "" : row.getConfigValue();
    }
}
