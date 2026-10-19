package com.suzhong.njl.service.admin;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.AuditLog;
import com.suzhong.njl.domain.enums.ActorType;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.mapper.AuditLogMapper;

/**
 * 运营审计写入器（运营端）。
 *
 * <p>职责：把运营的关键动作写入 {@code audit_log}，统一补齐 actor 类型、动作、目标类型与 JSON 详情，
 * 供各运营服务在业务写路径中调用，避免每个服务各拼一遍审计实体。
 *
 * <p>事务约定：本类不开事务，跟随调用方事务提交或回滚。调用方回滚时审计一并回滚，
 * 不会留下「操作没生效但流水还在」的记录。
 *
 * <p>降级约定：详情序列化失败只把 {@code detail_json} 置空，不阻断业务；插入失败直接向上抛，
 * 由调用方事务一起回滚。
 */
@Component
public class AuditRecorder {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditRecorder(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 写一条运营审计。
     *
     * <p>actor 固定为 {@code ADMIN}，{@code detail} 为 null 或空时 {@code detail_json} 写 null。
     * 目标 ID 允许为 null（例如站点配置没有单条目标）。
     *
     * @param adminId  当前运营账号 ID
     * @param action   动作类型，决定流水归类
     * @param target   目标类型
     * @param targetId 目标主键，可为 null
     * @param detail   审计详情，键值会被序列化为 JSON；可为 null
     */
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

    /** 序列化审计详情；空 map 或序列化失败都返回 null，不阻断业务。 */
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
