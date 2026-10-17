package com.suzhong.njl.service.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;

public final class CityRules {

    public static final String AUTO_FIELD_MESSAGE = "供给达标与攻略达标按实时计数自动勾选，不能手改";
    public static final String EMPTY_HINT = "该目的地暂未开通";
    public static final int DEFAULT_MIN_STORES = 5;
    public static final int DEFAULT_MIN_ARTICLES = 1;

    private CityRules() {
    }

    public static RegionOpenStatus effective(RegionOpenStatus status) {
        return status == null ? RegionOpenStatus.CLOSED : status;
    }

    public static boolean isOpen(Region region) {
        return region != null
                && region.getLevel() == RegionLevel.CITY
                && Boolean.TRUE.equals(region.getEnabled())
                && region.getOpenStatus() == RegionOpenStatus.OPEN;
    }

    public static int positiveOrDefault(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static List<String> missingReasons(
            boolean destinationReady,
            boolean supportCovered,
            boolean dirtyDataCleared,
            long storeCount,
            long articleCount,
            int minStores,
            int minArticles) {
        List<String> reasons = new ArrayList<>();
        if (!destinationReady) {
            reasons.add("目的地数据未齐");
        }
        if (storeCount < minStores) {
            reasons.add("供给未达标（当前 " + storeCount + " / 需要 " + minStores + "）");
        }
        if (articleCount < minArticles) {
            reasons.add("攻略未达标（当前 " + articleCount + " / 需要 " + minArticles + "）");
        }
        if (!supportCovered) {
            reasons.add("客服覆盖未说明");
        }
        if (!dirtyDataCleared) {
            reasons.add("测数或脏数据未确认清理");
        }
        return List.copyOf(reasons);
    }

    public static String openRejectedMessage(List<String> reasons) {
        return "未达标，不能开通：" + String.join("；", reasons);
    }

    /**
     * 意向归属的市。市本身，或区县的父市。挂省、地区缺失、父节点不是市，则没有归属。
     */
    public static Long owningCityId(Region region, Map<Long, Region> byId) {
        if (region == null || region.getLevel() == null) {
            return null;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            return region.getId();
        }
        if (region.getLevel() == RegionLevel.DISTRICT && region.getParentId() != null && byId != null) {
            Region parent = byId.get(region.getParentId());
            if (parent != null && parent.getLevel() == RegionLevel.CITY) {
                return parent.getId();
            }
        }
        return null;
    }

    /**
     * 店或攻略是否计入该市门槛。地区必须启用，且挂在该市或父节点是该市的区县上。
     */
    public static boolean countsTowardCity(Region region, long cityId) {
        if (region == null || !Boolean.TRUE.equals(region.getEnabled()) || region.getLevel() == null) {
            return false;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            return region.getId() != null && region.getId() == cityId;
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            return region.getParentId() != null && region.getParentId() == cityId;
        }
        return false;
    }
}
