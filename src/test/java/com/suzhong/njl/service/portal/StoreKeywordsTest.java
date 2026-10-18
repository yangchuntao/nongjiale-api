package com.suzhong.njl.service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.web.ApiException;

class StoreKeywordsTest {

    @Test
    void blankIsOmittedAndWildcardsAreEscaped() {
        assertThat(StoreKeywords.normalize(null)).isNull();
        assertThat(StoreKeywords.normalize("  ")).isNull();
        assertThat(StoreKeywords.normalize(" 表弟 ")).isEqualTo("表弟");
        assertThat(StoreKeywords.normalize("100%_!")).isEqualTo("100!%!_!!");
    }

    @Test
    void longerThan64IsRejected() {
        assertThatThrownBy(() -> StoreKeywords.normalize("a".repeat(65)))
                .isInstanceOf(ApiException.class)
                .hasMessage("参数不正确");
        assertThat(StoreKeywords.normalize("a".repeat(64))).hasSize(64);
    }
}
