package com.suzhong.njl.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.suzhong.njl.domain.enums.BannerMessage;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

class BannerRulesTest {

    @Test
    void createDefaultsSortAndEnabledAndClearsBlankTitle() {
        BannerRules.Prepared prepared = BannerRules.prepare(request("  周末  ", " https://img.example/a.png ", " /stores ", BannerScope.ALL, null, null), false);

        assertThat(prepared.title()).isEqualTo("周末");
        assertThat(prepared.imageUrl()).isEqualTo("https://img.example/a.png");
        assertThat(prepared.linkUrl()).isEqualTo("/stores");
        assertThat(prepared.scope()).isEqualTo(BannerScope.ALL);
        assertThat(prepared.cityId()).isNull();
        assertThat(prepared.sortOrder()).isEqualTo(100);
        assertThat(BannerRules.enabled(null, false)).isTrue();
        assertThat(BannerRules.enabled(Boolean.FALSE, false)).isFalse();
        assertThat(BannerRules.keyword(null)).isNull();
        assertThat(BannerRules.keyword("  ")).isNull();
        assertThat(BannerRules.keyword(" 扬州 ")).isEqualTo("扬州");
    }

    @Test
    void rejectsFieldsInOrder() {
        assertMessage(request(null, "  ", "/ok", BannerScope.ALL, null, 1), BannerMessage.IMAGE_REQUIRED);
        assertMessage(request(null, "x".repeat(513), "/ok", BannerScope.ALL, null, 1), BannerMessage.IMAGE_TOO_LONG);
        assertMessage(request(null, "javascript:alert(1)", "/ok", BannerScope.ALL, null, 1), BannerMessage.IMAGE_SCHEME);
        assertMessage(request(null, "data:image/png;base64,aaaa", "/ok", BannerScope.ALL, null, 1), BannerMessage.IMAGE_SCHEME);
        assertMessage(request(null, "/relative.png", "/ok", BannerScope.ALL, null, 1), BannerMessage.IMAGE_SCHEME);
        assertMessage(request(null, "https://img.example/a.png", "  ", BannerScope.ALL, null, 1), BannerMessage.LINK_REQUIRED);
        assertMessage(request(null, "https://img.example/a.png", "y".repeat(513), BannerScope.ALL, null, 1), BannerMessage.LINK_TOO_LONG);
        assertMessage(request(null, "https://img.example/a.png", "//host", BannerScope.ALL, null, 1), BannerMessage.LINK_INVALID);
        assertMessage(request(null, "https://img.example/a.png", "javascript:alert(1)", BannerScope.ALL, null, 1), BannerMessage.LINK_INVALID);
        assertMessage(request(null, "https://img.example/a.png", "data:text/html,hi", BannerScope.ALL, null, 1), BannerMessage.LINK_INVALID);
        assertMessage(request(null, "https://img.example/a.png", "http://example.com/a b", BannerScope.ALL, null, 1), BannerMessage.LINK_INVALID);
        assertMessage(request(null, "https://img.example/a.png", "/a\\b", BannerScope.ALL, null, 1), BannerMessage.LINK_INVALID);
        assertMessage(request("题".repeat(65), "https://img.example/a.png", "/", BannerScope.ALL, null, 1), BannerMessage.TITLE_TOO_LONG);
        assertMessage(request(null, "https://img.example/a.png", "/", BannerScope.ALL, null, -1), BannerMessage.SORT_INVALID);
        assertMessage(request(null, "https://img.example/a.png", "/", BannerScope.ALL, null, 1_000_001), BannerMessage.SORT_INVALID);
        assertMessage(request(null, "http://img.example/a.png", "https://example.com", null, null, 0), BannerMessage.SCOPE_REQUIRED);
        assertMessage(request(null, "http://img.example/a.png", "/?city=yangzhou", BannerScope.ALL, 2L, 1_000_000), BannerMessage.ALL_WITH_CITY);
        assertMessage(request(" ", "http://img.example/a.png", "/#top", BannerScope.CITY, null, 0), BannerMessage.CITY_REQUIRED);
    }

    @Test
    void updateRequiresSortAndEnabled() {
        BannerWriteRequest missingSort = request(null, "https://img.example/a.png", "/", BannerScope.ALL, null, null);
        assertThatThrownBy(() -> BannerRules.prepare(missingSort, true))
                .isInstanceOf(ApiException.class)
                .hasMessage(BannerMessage.SORT_INVALID.text());
        assertThatThrownBy(() -> BannerRules.enabled(null, true))
                .isInstanceOf(ApiException.class)
                .hasMessage(BannerMessage.ENABLED_REQUIRED.text());
        assertThatThrownBy(() -> BannerRules.keyword("k".repeat(65)))
                .isInstanceOf(ApiException.class)
                .hasMessage("参数不正确");
    }

    private static void assertMessage(BannerWriteRequest request, BannerMessage message) {
        assertThatThrownBy(() -> BannerRules.prepare(request, false))
                .isInstanceOf(ApiException.class)
                .hasMessage(message.text());
    }

    private static BannerWriteRequest request(
            String title,
            String imageUrl,
            String linkUrl,
            BannerScope scope,
            Long cityId,
            Integer sortOrder) {
        return new BannerWriteRequest(title, imageUrl, linkUrl, scope, cityId, sortOrder, null);
    }
}
