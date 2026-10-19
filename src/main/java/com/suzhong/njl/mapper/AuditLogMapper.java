package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.AuditLog;
/**
 * 审计流水 Mapper，对应 {@code audit_log} 表。
 *
 * <p>无自定义 SQL。该表只追加、不更新也不删除，审核、代上下架、状态流转等动作各写一条，
 * 便于事后追溯「谁在什么时候改了什么」。
 */
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
