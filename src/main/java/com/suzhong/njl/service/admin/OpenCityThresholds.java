package com.suzhong.njl.service.admin;

import org.springframework.stereotype.Component;

import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.service.city.CityRules;

@Component
public class OpenCityThresholds {

    private final SiteConfigMapper siteConfigMapper;

    public OpenCityThresholds(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    public int minStores() {
        return CityRules.positiveOrDefault(read(SiteConfigKey.OPEN_CITY_MIN_STORES), CityRules.DEFAULT_MIN_STORES);
    }

    public int minArticles() {
        return CityRules.positiveOrDefault(read(SiteConfigKey.OPEN_CITY_MIN_ARTICLES), CityRules.DEFAULT_MIN_ARTICLES);
    }

    private String read(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null ? null : row.getConfigValue();
    }
}
