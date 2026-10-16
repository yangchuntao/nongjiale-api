package com.suzhong.njl.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.enums.RegionLevel;

class RegionLevelTest {

    @Test
    void codesAndLabelsStayOnTheSeedScale() {
        assertThat(RegionLevel.PROVINCE.code()).isEqualTo(1);
        assertThat(RegionLevel.CITY.code()).isEqualTo(2);
        assertThat(RegionLevel.DISTRICT.code()).isEqualTo(3);
        assertThat(RegionLevel.PROVINCE.label()).isEqualTo("省");
        assertThat(RegionLevel.CITY.label()).isEqualTo("市");
        assertThat(RegionLevel.DISTRICT.label()).isEqualTo("区县");
    }

    @Test
    void fromCodeRejectsUnknownLevels() {
        assertThat(RegionLevel.fromCode(2)).isEqualTo(RegionLevel.CITY);
        assertThatThrownBy(() -> RegionLevel.fromCode(9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知地区层级");
    }
}
