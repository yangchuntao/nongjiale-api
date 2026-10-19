package com.suzhong.njl.service.portal;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 找店关键词。空串当省略。转义 LIKE 通配，避免用户输入变成 % 或 _。
 *
 * <p>规则来源：README「门户」章节，关键词按店名包含匹配，超过 64 字返回 400「参数不正确」。
 */
public final class StoreKeywords {

    /** 关键词长度上限（按字符数，trim 之后计算），超出直接 400，不截断。 */
    public static final int MAX_LENGTH = 64;

    private StoreKeywords() {
    }

    /**
     * 规范化关键词：去首尾空白 → 空值归一为 null → 长度校验 → 转义 LIKE 通配符。
     *
     * <p>返回值已经是可安全拼进 {@code LIKE CONCAT('%', ?, '%') ESCAPE '!'} 的文本，
     * 调用方不要再转义第二次。
     *
     * @param raw 原始关键词，可为 null
     * @return 转义后的关键词；空白输入返回 null
     * @throws ApiException 去除空白后超过 64 字时 400「参数不正确」
     */
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

    /**
     * 用感叹号做转义符（与 SQL 的 {@code ESCAPE '!'} 对应）：先转义转义符自身，再转义 % 与 _。
     *
     * <p>感叹号是 SQL 里 {@code ESCAPE '!'} 指定的转义符，因此三类字符都要处理，
     * 只转 {@code %} 和 {@code _} 会让用户输入的 {@code !} 反过来吞掉后一个字符。
     */
    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
