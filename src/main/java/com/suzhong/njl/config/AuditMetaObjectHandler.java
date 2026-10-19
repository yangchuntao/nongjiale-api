package com.suzhong.njl.config;

import java.time.Clock;
import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

/**
 * 审计时间字段自动填充器。
 *
 * <p>配合实体上的 {@code @TableField(fill = INSERT)} / {@code INSERT_UPDATE}：
 * 插入时补齐 {@code createdAt} 与 {@code updatedAt}，更新时刷新 {@code updatedAt}，
 * 业务代码不必手写时间。时间统一取注入的 {@code Clock}，保证与 {@code Asia/Shanghai} 一致，
 * 测试里也能换成固定时钟。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final Clock applicationClock;

    public AuditMetaObjectHandler(Clock applicationClock) {
        this.applicationClock = applicationClock;
    }

    /** 插入填充：只在字段为 null 时写入，允许导入或种子数据显式指定历史时间。 */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(applicationClock);
        fillIfNull(metaObject, "createdAt", now);
        fillIfNull(metaObject, "updatedAt", now);
    }

    /** 更新填充：无条件覆盖 {@code updatedAt}，防止调用方漏改导致时间戳失真。 */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updatedAt")) {
            metaObject.setValue("updatedAt", LocalDateTime.now(applicationClock));
        }
    }

    /** 字段存在且有 setter、当前值为 null 时才填充。 */
    private void fillIfNull(MetaObject metaObject, String field, LocalDateTime now) {
        if (metaObject.hasSetter(field) && metaObject.getValue(field) == null) {
            metaObject.setValue(field, now);
        }
    }
}
