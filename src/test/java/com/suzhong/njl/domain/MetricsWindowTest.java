package com.suzhong.njl.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.enums.MetricsWindow;

class MetricsWindowTest {

    @Test
    void parsesSevenThirtyAndAllAndRejectsAnythingElse() {
        assertThat(MetricsWindow.parse(null)).isEqualTo(MetricsWindow.DAYS_7);
        assertThat(MetricsWindow.parse("  ")).isEqualTo(MetricsWindow.DAYS_7);
        assertThat(MetricsWindow.parse("7").code()).isEqualTo("7");
        assertThat(MetricsWindow.parse("30").code()).isEqualTo("30");
        assertThat(MetricsWindow.parse(" ALL ").code()).isEqualTo("ALL");
        assertThatThrownBy(() -> MetricsWindow.parse("1")).hasMessage("参数不正确");
    }

    @Test
    void windowIsRollingNotCalendarMidnight() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-17T06:30:00Z"), ZoneId.of("Asia/Shanghai"));
        OffsetDateTime from = MetricsWindow.DAYS_7.from(clock);
        assertThat(from).isEqualTo(OffsetDateTime.parse("2026-10-10T14:30:00+08:00"));
        assertThat(MetricsWindow.DAYS_30.from(clock)).isEqualTo(OffsetDateTime.parse("2026-09-17T14:30:00+08:00"));
        assertThat(MetricsWindow.ALL.from(clock)).isNull();
    }
}
