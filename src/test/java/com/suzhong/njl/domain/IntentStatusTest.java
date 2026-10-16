package com.suzhong.njl.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.enums.IntentStatus;

class IntentStatusTest {

    @Test
    void pendingCanConfirmRescheduleOrClose() {
        assertThat(IntentStatus.PENDING.canTransitTo(IntentStatus.CONFIRMED)).isTrue();
        assertThat(IntentStatus.PENDING.canTransitTo(IntentStatus.RESCHEDULED)).isTrue();
        assertThat(IntentStatus.PENDING.canTransitTo(IntentStatus.CLOSED)).isTrue();
        assertThat(IntentStatus.PENDING.canTransitTo(IntentStatus.CHECKED_IN)).isFalse();
    }

    @Test
    void confirmedAndRescheduledFollowTheLockedMachine() {
        assertThat(IntentStatus.CONFIRMED.canTransitTo(IntentStatus.CHECKED_IN)).isTrue();
        assertThat(IntentStatus.CONFIRMED.canTransitTo(IntentStatus.CLOSED)).isTrue();
        assertThat(IntentStatus.CONFIRMED.canTransitTo(IntentStatus.RESCHEDULED)).isFalse();
        assertThat(IntentStatus.RESCHEDULED.canTransitTo(IntentStatus.CONFIRMED)).isTrue();
        assertThat(IntentStatus.RESCHEDULED.canTransitTo(IntentStatus.CLOSED)).isTrue();
        assertThat(IntentStatus.RESCHEDULED.canTransitTo(IntentStatus.CHECKED_IN)).isFalse();
    }

    @Test
    void terminalsDoNotMove() {
        assertThat(IntentStatus.CHECKED_IN.terminal()).isTrue();
        assertThat(IntentStatus.CLOSED.terminal()).isTrue();
        assertThat(IntentStatus.CHECKED_IN.canTransitTo(IntentStatus.CLOSED)).isFalse();
        assertThat(IntentStatus.CLOSED.canTransitTo(IntentStatus.PENDING)).isFalse();
        assertThat(IntentStatus.CHECKED_IN.label()).isEqualTo("已达成");
    }
}
