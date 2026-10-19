package com.suzhong.njl.domain.enums;

/**
 * 审计目标类型。落在 audit_log.target_type，不是状态机。
 */
/**
 * 审计目标类型。落在 audit_log.target_type，不是状态机。
 *
 * <p>本枚举没有 {@code @EnumValue}，实体 {@code AuditLog.targetType} 是字符串，
 * 由 {@code AuditRecorder} 显式调用 {@link #code()} 写库；与 {@code target_id}
 * 一起定位被操作的对象。
 */
public enum AuditTarget {
    /** 商家入驻申请，对应 {@code merchant_apply.id}。 */
    MERCHANT_APPLY("MERCHANT_APPLY"),
    /** 商家主体，对应 {@code merchant_org.id}。 */
    MERCHANT_ORG("MERCHANT_ORG"),
    /** 门店，对应 {@code store.id}。 */
    STORE("STORE"),
    /** 上下架申请，对应 {@code shelf_application.id}。 */
    SHELF_APPLICATION("SHELF_APPLICATION"),
    /** 预订意向，对应 {@code booking_intent.id}。 */
    BOOKING_INTENT("BOOKING_INTENT"),
    /** 站点配置，对应 {@code site_config.config_key} 所属记录。 */
    SITE_CONFIG("SITE_CONFIG"),
    /** 地区（市），对应 {@code region.id}，用于开城/关城审计。 */
    REGION("REGION"),
    /** 首页 Banner，对应 {@code home_banner.id}。 */
    HOME_BANNER("HOME_BANNER");

    private final String code;

    AuditTarget(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
