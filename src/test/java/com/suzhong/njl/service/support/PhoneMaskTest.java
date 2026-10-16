package com.suzhong.njl.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneMaskTest {

    @Test
    void merchantContactUsesMiddleFourDigits() {
        assertThat(PhoneMask.maskContact("13812348000")).isEqualTo("138****8000");
        assertThat(PhoneMask.maskContact("13812348000")).doesNotContain("1234");
    }

    @Test
    void shortOrMissingPhonesStayAsIs() {
        assertThat(PhoneMask.mask(null)).isNull();
        assertThat(PhoneMask.mask("123456")).isEqualTo("123456");
        assertThat(PhoneMask.maskContact(null)).isNull();
    }

    @Test
    void contactThatDoesNotMatchElevenDigitsFallsBackToMask() {
        assertThat(PhoneMask.maskContact("138123480001")).isEqualTo("138****0001");
    }
}
