package com.suzhong.njl.config;

import java.time.Clock;
import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final Clock applicationClock;

    public AuditMetaObjectHandler(Clock applicationClock) {
        this.applicationClock = applicationClock;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(applicationClock);
        fillIfNull(metaObject, "createdAt", now);
        fillIfNull(metaObject, "updatedAt", now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updatedAt")) {
            metaObject.setValue("updatedAt", LocalDateTime.now(applicationClock));
        }
    }

    private void fillIfNull(MetaObject metaObject, String field, LocalDateTime now) {
        if (metaObject.hasSetter(field) && metaObject.getValue(field) == null) {
            metaObject.setValue(field, now);
        }
    }
}
