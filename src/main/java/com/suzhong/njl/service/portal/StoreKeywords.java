package com.suzhong.njl.service.portal;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 找店关键词。空串当省略。转义 LIKE 通配，避免用户输入变成 % 或 _。
 */
public final class StoreKeywords {

    public static final int MAX_LENGTH = 64;

    private StoreKeywords() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数不正确");
        }
        return escapeLike(trimmed);
    }

    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
