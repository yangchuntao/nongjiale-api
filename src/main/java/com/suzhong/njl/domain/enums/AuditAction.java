package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 审计日志的动作类型。
 *
 * <p>落在 {@code audit_log.action} 列，存 {@link EnumValue} 标注的 {@code code}；
 * 与 {@link ActorType}、{@link AuditTarget} 组合描述「谁对哪类对象做了什么」。
 * 本枚举只做流水归类，不是业务状态机，不改写业务表；前端展示动作名取 {@link #label()}。
 */
public enum AuditAction {
    /** 入驻通过：运营审核商家入驻申请通过并开通账号。 */
    APPLY_APPROVE("APPLY_APPROVE", "入驻通过"),
    /** 入驻驳回：运营驳回商家入驻申请，需带原因。 */
    APPLY_REJECT("APPLY_REJECT", "入驻驳回"),
    /** 代上下架：运营在门店编辑中直接改 {@code shelfStatus}，不走申请队列。 */
    SHELF_FORCE("SHELF_FORCE", "代上下架"),
    /** 审上下架：运营审批商家提交的上下架申请。 */
    SHELF_REVIEW("SHELF_REVIEW", "审上下架"),
    /** 意向状态：运营变更预订意向状态。 */
    INTENT_STATUS("INTENT_STATUS", "意向状态"),
    /** 站点配置：运营更新站名、主色、客服电话等配置。 */
    SITE_CONFIG_UPDATE("SITE_CONFIG_UPDATE", "站点配置"),
    /** 商家停用：运营把商家主体或账号置为不可用。 */
    MERCHANT_DISABLE("MERCHANT_DISABLE", "商家停用"),
    /** 商家启用：运营恢复商家主体或账号。 */
    MERCHANT_ENABLE("MERCHANT_ENABLE", "商家启用"),
    /** 开城清单：运营手工调整开城三项门槛标记。 */
    CITY_CHECKLIST("CITY_CHECKLIST", "开城清单"),
    /** 开通城市：运营把市的 {@code open_status} 置为 {@code OPEN}。 */
    CITY_OPEN("CITY_OPEN", "开通城市"),
    /** 关闭城市：运营把市的 {@code open_status} 置为 {@code CLOSED}，历史数据保留。 */
    CITY_CLOSE("CITY_CLOSE", "关闭城市"),
    /** 新建 Banner：运营创建首页广告位。 */
    BANNER_CREATE("BANNER_CREATE", "新建 Banner"),
    /** 更新 Banner：运营全量更新广告位内容。 */
    BANNER_UPDATE("BANNER_UPDATE", "更新 Banner"),
    /** 启用 Banner：运营把广告位置为启用。 */
    BANNER_ENABLE("BANNER_ENABLE", "启用 Banner"),
    /** 停用 Banner：运营把广告位置为停用。 */
    BANNER_DISABLE("BANNER_DISABLE", "停用 Banner"),
    /** 删除 Banner：运营物理删除广告位。 */
    BANNER_DELETE("BANNER_DELETE", "删除 Banner");

    @EnumValue
    private final String code;
    private final String label;

    AuditAction(String code, String label) {
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
