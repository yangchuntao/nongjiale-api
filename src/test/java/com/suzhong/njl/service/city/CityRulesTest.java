package com.suzhong.njl.service.city;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;

class CityRulesTest {

    @Test
    void nullOpenStatusCountsAsClosedAndOnlyEnabledCityIsOpen() {
        assertThat(CityRules.effective(null)).isEqualTo(RegionOpenStatus.CLOSED);
        assertThat(CityRules.effective(RegionOpenStatus.OPEN)).isEqualTo(RegionOpenStatus.OPEN);

        assertThat(CityRules.isOpen(null)).isFalse();
        assertThat(CityRules.isOpen(region(1L, RegionLevel.CITY, false, RegionOpenStatus.OPEN, null))).isFalse();
        assertThat(CityRules.isOpen(region(1L, RegionLevel.CITY, true, null, null))).isFalse();
        assertThat(CityRules.isOpen(region(1L, RegionLevel.CITY, true, RegionOpenStatus.CLOSED, null))).isFalse();
        assertThat(CityRules.isOpen(region(1L, RegionLevel.DISTRICT, true, RegionOpenStatus.OPEN, 2L))).isFalse();
        assertThat(CityRules.isOpen(region(1L, RegionLevel.CITY, true, RegionOpenStatus.OPEN, 9L))).isTrue();
    }

    @Test
    void thresholdFallsBackUnlessPositiveInteger() {
        assertThat(CityRules.positiveOrDefault(null, 5)).isEqualTo(5);
        assertThat(CityRules.positiveOrDefault("  ", 5)).isEqualTo(5);
        assertThat(CityRules.positiveOrDefault("0", 5)).isEqualTo(5);
        assertThat(CityRules.positiveOrDefault("-3", 1)).isEqualTo(1);
        assertThat(CityRules.positiveOrDefault("abc", 5)).isEqualTo(5);
        assertThat(CityRules.positiveOrDefault(" 8 ", 5)).isEqualTo(8);
    }

    @Test
    void missingReasonsKeepFixedOrder() {
        assertThat(CityRules.missingReasons(false, false, false, 1, 0, 5, 1)).containsExactly(
                "目的地数据未齐",
                "供给未达标（当前 1 / 需要 5）",
                "攻略未达标（当前 0 / 需要 1）",
                "客服覆盖未说明",
                "测数或脏数据未确认清理");
        assertThat(CityRules.missingReasons(true, true, true, 5, 1, 5, 1)).isEmpty();
        assertThat(CityRules.openRejectedMessage(CityRules.missingReasons(true, true, true, 1, 0, 5, 1)))
                .isEqualTo("未达标，不能开通：供给未达标（当前 1 / 需要 5）；攻略未达标（当前 0 / 需要 1）");
    }

    @Test
    void owningCityIgnoresProvinceAndNonCityParent() {
        Region province = region(1L, RegionLevel.PROVINCE, true, null, null);
        Region city = region(2L, RegionLevel.CITY, true, RegionOpenStatus.CLOSED, 1L);
        Region district = region(3L, RegionLevel.DISTRICT, true, null, 2L);
        Region orphan = region(4L, RegionLevel.DISTRICT, true, null, 1L);
        Region noParent = region(5L, RegionLevel.DISTRICT, false, null, null);
        Map<Long, Region> byId = Map.of(1L, province, 2L, city, 3L, district, 4L, orphan);

        assertThat(CityRules.owningCityId(null, byId)).isNull();
        assertThat(CityRules.owningCityId(province, byId)).isNull();
        assertThat(CityRules.owningCityId(city, byId)).isEqualTo(2L);
        assertThat(CityRules.owningCityId(district, byId)).isEqualTo(2L);
        assertThat(CityRules.owningCityId(orphan, byId)).isNull();
        assertThat(CityRules.owningCityId(noParent, byId)).isNull();
        assertThat(CityRules.owningCityId(district, null)).isNull();
    }

    @Test
    void supplyCountRequiresEnabledCityOrItsDistrict() {
        Region city = region(2L, RegionLevel.CITY, true, RegionOpenStatus.OPEN, 1L);
        Region district = region(3L, RegionLevel.DISTRICT, true, null, 2L);
        Region disabledDistrict = region(4L, RegionLevel.DISTRICT, false, null, 2L);
        Region province = region(1L, RegionLevel.PROVINCE, true, null, null);

        assertThat(CityRules.countsTowardCity(city, 2L)).isTrue();
        assertThat(CityRules.countsTowardCity(district, 2L)).isTrue();
        assertThat(CityRules.countsTowardCity(district, 9L)).isFalse();
        assertThat(CityRules.countsTowardCity(disabledDistrict, 2L)).isFalse();
        assertThat(CityRules.countsTowardCity(province, 2L)).isFalse();
        assertThat(CityRules.countsTowardCity(null, 2L)).isFalse();
    }

    private Region region(Long id, RegionLevel level, boolean enabled, RegionOpenStatus openStatus, Long parentId) {
        Region region = new Region();
        region.setId(id);
        region.setLevel(level);
        region.setEnabled(enabled);
        region.setOpenStatus(openStatus);
        region.setParentId(parentId);
        return region;
    }
}
