package com.suzhong.njl.domain.enums;

/**
 * 审计目标类型。落在 audit_log.target_type，不是状态机。
 */
public enum AuditTarget {
    MERCHANT_APPLY("MERCHANT_APPLY"),
    MERCHANT_ORG("MERCHANT_ORG"),
    STORE("STORE"),
    SHELF_APPLICATION("SHELF_APPLICATION"),
    BOOKING_INTENT("BOOKING_INTENT"),
    SITE_CONFIG("SITE_CONFIG");

    private final String code;

    AuditTarget(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
