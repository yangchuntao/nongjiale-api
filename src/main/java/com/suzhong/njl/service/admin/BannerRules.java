package com.suzhong.njl.service.admin;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.enums.BannerMessage;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

/**
 * 创建与更新共用的字段校验。市是否开通留在服务里，排在是否启用之前。
 */
public final class BannerRules {

    public static final int IMAGE_MAX = 512;
    public static final int LINK_MAX = 512;
    public static final int TITLE_MAX = 64;
    public static final int SORT_MIN = 0;
    public static final int SORT_MAX = 1_000_000;
    public static final int DEFAULT_SORT = 100;
    public static final int KEYWORD_MAX = 64;

    private BannerRules() {
    }

    public record Prepared(
            String title,
            String imageUrl,
            String linkUrl,
            BannerScope scope,
            Long cityId,
            int sortOrder) {
    }

    public static Prepared prepare(BannerWriteRequest request, boolean updating) {
        String imageUrl = requiredHttpUrl(request.imageUrl());
        String linkUrl = requiredLink(request.linkUrl());
        String title = title(request.title());
        int sortOrder = sortOrder(request.sortOrder(), updating);
        BannerScope scope = request.scope();
        if (scope == null) {
            throw BannerMessage.SCOPE_REQUIRED.toException();
        }
        Long cityId = request.cityId();
        if (scope == BannerScope.ALL && cityId != null) {
            throw BannerMessage.ALL_WITH_CITY.toException();
        }
        if (scope == BannerScope.CITY && cityId == null) {
            throw BannerMessage.CITY_REQUIRED.toException();
        }
        if (scope == BannerScope.ALL) {
            cityId = null;
        }
        return new Prepared(title, imageUrl, linkUrl, scope, cityId, sortOrder);
    }

    public static boolean enabled(Boolean raw, boolean updating) {
        if (raw == null) {
            if (updating) {
                throw BannerMessage.ENABLED_REQUIRED.toException();
            }
            return true;
        }
        return Boolean.TRUE.equals(raw);
    }

    /**
     * 空串当省略。超长用现网「参数不正确」，不进 {@link BannerMessage}。
     */
    public static String keyword(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > KEYWORD_MAX) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数不正确");
        }
        return trimmed;
    }

    private static String requiredHttpUrl(String raw) {
        String imageUrl = trimToNull(raw);
        if (imageUrl == null) {
            throw BannerMessage.IMAGE_REQUIRED.toException();
        }
        if (imageUrl.length() > IMAGE_MAX) {
            throw BannerMessage.IMAGE_TOO_LONG.toException();
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw BannerMessage.IMAGE_SCHEME.toException();
        }
        return imageUrl;
    }

    private static String requiredLink(String raw) {
        String linkUrl = trimToNull(raw);
        if (linkUrl == null) {
            throw BannerMessage.LINK_REQUIRED.toException();
        }
        if (linkUrl.length() > LINK_MAX) {
            throw BannerMessage.LINK_TOO_LONG.toException();
        }
        if (!acceptableLink(linkUrl)) {
            throw BannerMessage.LINK_INVALID.toException();
        }
        return linkUrl;
    }

    private static String title(String raw) {
        String title = trimToNull(raw);
        if (title != null && title.length() > TITLE_MAX) {
            throw BannerMessage.TITLE_TOO_LONG.toException();
        }
        return title;
    }

    private static int sortOrder(Integer raw, boolean updating) {
        if (raw == null) {
            if (updating) {
                throw BannerMessage.SORT_INVALID.toException();
            }
            return DEFAULT_SORT;
        }
        if (raw < SORT_MIN || raw > SORT_MAX) {
            throw BannerMessage.SORT_INVALID.toException();
        }
        return raw;
    }

    static boolean acceptableLink(String link) {
        if (hasWhitespace(link)) {
            return false;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            return true;
        }
        return link.startsWith("/")
                && (link.length() == 1 || link.charAt(1) != '/')
                && link.indexOf('\\') < 0;
    }

    private static boolean hasWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
