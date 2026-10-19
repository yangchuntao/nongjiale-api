package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 审计日志的操作者类型。
 *
 * <p>落在 {@code audit_log.actor_type} 列；该列存 {@link EnumValue} 标注的 {@code code}，
 * 不是 {@code label}。本枚举只用于区分「谁做了这次操作」，不构成状态机，
 * 前端展示审计明细时取 {@link #label()}。
 */
public enum ActorType {
    /** 运营：由运营后台（{@code OPERATOR} 或 {@code AGENT} 账号）发起的审核、代编、开城等操作。 */
    ADMIN("ADMIN", "运营"),
    /** 商家：预留给商家端账号发起的操作；当前 {@code AuditRecorder} 尚未写入该值。 */
    MERCHANT("MERCHANT", "商家"),
    /** 系统：预留给无登录用户的自动动作，当前审计写入只用 {@code ADMIN}，暂无落库记录。 */
    SYSTEM("SYSTEM", "系统");

    @EnumValue
    private final String code;
    private final String label;

    ActorType(String code, String label) {
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
