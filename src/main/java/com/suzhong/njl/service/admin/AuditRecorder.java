package com.suzhong.njl.service.admin;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.AuditLog;
import com.suzhong.njl.domain.enums.ActorType;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.mapper.AuditLogMapper;

@Component
public class AuditRecorder {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditRecorder(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(Long adminId, AuditAction action, AuditTarget target, Long targetId, Map<String, ?> detail) {
        AuditLog row = new AuditLog();
        row.setActorType(ActorType.ADMIN);
        row.setActorId(adminId);
        row.setAction(action);
        row.setTargetType(target.code());
        row.setTargetId(targetId);
        row.setDetailJson(write(detail));
        auditLogMapper.insert(row);
    }

    private String write(Map<String, ?> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            return null;
        }
    }
}
