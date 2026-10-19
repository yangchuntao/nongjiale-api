package com.suzhong.njl.service.admin;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.enums.BannerMessage;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

/**
 * Banner 字段校验规则（运营端）。
 *
 * <p>职责：把创建/更新共用的字段规范化与顺序校验收敛为纯函数，顺序为
 * 图片 → 跳转链接 → 标题 → 排序 → 投放范围。校验顺序即契约，前端按第一个命中的错误提示。
 *
 * <p>市是否存在、是否启用、是否开通不在本类：由 {@code AdminBannerService} 在保存前校验，
 * 且排在启用态校验之前。创建与更新的差异只在「省略排序/启用态是否允许」，新建可省并取默认值，
 * 更新必须显式传。
 *
 * <p>本类不查库、不开事务、不写审计。
 */
public final class BannerRules {

    /** 图片地址长度上限（字符）。 */
    public static final int IMAGE_MAX = 512;
    /** 跳转链接长度上限（字符）。 */
    public static final int LINK_MAX = 512;
    /** 运营标题长度上限（字符）。 */
    public static final int TITLE_MAX = 64;
    /** 排序值下限。 */
    public static final int SORT_MIN = 0;
    /** 排序值上限，与产品约定一致。 */
    public static final int SORT_MAX = 1_000_000;
    /** 新建时省略排序的默认值，越小越靠前。 */
    public static final int DEFAULT_SORT = 100;
    /** 列表关键词长度上限（字符）。 */
    public static final int KEYWORD_MAX = 64;

    private BannerRules() {
    }

    /**
     * 校验并规范化后的 Banner 写入字段。
     *
     * @param title     去除首尾空白后的标题，空串转 null
     * @param imageUrl  图片地址，非空且为 http/https
     * @param linkUrl   跳转链接，站内路径或 http(s) 外链
     * @param scope     投放范围，{@code ALL} 或 {@code CITY}
     * @param cityId    指定市 ID；全站投放时恒为 null
     * @param sortOrder 排序值，0 到 1000000
     */
    public record Prepared(
            String title,
            String imageUrl,
            String linkUrl,
            BannerScope scope,
            Long cityId,
            int sortOrder) {
    }

    /**
     * 校验创建/更新的通用字段并规范化。
     *
     * <p>流程：图片 → 跳转链接 → 标题 → 排序 → 投放范围 → 范围与 cityId 的互斥关系。
     * 全站投放会强制把 {@code cityId} 置空，避免脏值落库。
     *
     * @param request  原始写入请求
     * @param updating true 表示更新：排序必填，省略按 400 处理；false 表示新建，省略取默认 100
     * @return 规范化后的字段
     * @throws com.suzhong.njl.web.ApiException 任一字段非法时 400，文案取自 {@link BannerMessage}
     */
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
        // 全站投放与指定市互斥：带了 cityId 直接拒绝，避免语义歧义
        if (scope == BannerScope.ALL && cityId != null) {
            throw BannerMessage.ALL_WITH_CITY.toException();
        }
        if (scope == BannerScope.CITY && cityId == null) {
            throw BannerMessage.CITY_REQUIRED.toException();
        }
        if (scope == BannerScope.ALL) {
            // 全站投放强制清空 cityId，防止历史脏值继续留在库里
            cityId = null;
        }
        return new Prepared(title, imageUrl, linkUrl, scope, cityId, sortOrder);
    }

    /**
     * 解析启用态：新建省略默认 true，更新省略按 400 拒绝。
     *
     * @param raw      请求中的启用值，可为 null
     * @param updating true 表示更新，必须显式传
     * @return 最终启用态
     * @throws com.suzhong.njl.web.ApiException 更新时未传 400「请指定是否启用」
     */
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
     * 规范化列表关键词：空串当省略。超长用现网「参数不正确」，不进 {@link BannerMessage}。
     *
     * @param raw 原始关键词，可为 null
     * @return 去除首尾空白后的关键词；空白返回 null
     * @throws com.suzhong.njl.web.ApiException 超过 64 字时 400「参数不正确」
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

    /** 校验图片地址：必填、不超长、必须是 http 或 https。 */
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

    /** 校验跳转链接：必填、不超长，且满足站内路径或 http(s) 外链规则。 */
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

    /** 规范化标题：空白转 null，超长 400。 */
    private static String title(String raw) {
        String title = trimToNull(raw);
        if (title != null && title.length() > TITLE_MAX) {
            throw BannerMessage.TITLE_TOO_LONG.toException();
        }
        return title;
    }

    /** 解析排序：新建省略取默认值，更新省略或越界都按 400 拒绝。 */
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

    /**
     * 跳转链接是否可接受：不含任何空白；http(s) 外链直接通过；
     * 站内路径必须以单个 {@code /} 开头（{@code //} 视为协议相对地址），且不能含反斜杠。
     */
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

    /** 是否含普通空白或 Unicode 空格类字符。 */
    private static boolean hasWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
                return true;
            }
        }
        return false;
    }

    /** 去除首尾空白，空串转 null。 */
    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
