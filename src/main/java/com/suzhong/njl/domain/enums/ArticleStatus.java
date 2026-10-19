package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 攻略文章的发布状态。
 *
 * <p>落在 {@code article.status} 列，存 {@link EnumValue} 标注的 {@code code}。
 * 状态机只有两态，由运营在后台直接切换：{@code DRAFT} 与 {@code PUBLISHED} 可互相流转，
 * 发布时写入发布时间。门户列表、首页只露出 {@code PUBLISHED}，
 * 已发布攻略即使所属城市后来关城，直链仍可打开。前端展示取 {@link #label()}。
 */
public enum ArticleStatus {
    /** 草稿：仅运营后台可见，门户列表、首页与搜索都不出现。 */
    DRAFT("DRAFT", "草稿"),
    /** 已发布：门户列表和首页可见；若所属城市未开通，仅在首页推荐中让位。 */
    PUBLISHED("PUBLISHED", "已发布");

    @EnumValue
    private final String code;
    private final String label;

    ArticleStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
