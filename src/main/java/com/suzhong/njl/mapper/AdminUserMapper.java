package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.AdminUser;
/**
 * 运营后台账号 Mapper，对应 {@code admin_user} 表。
 *
 * <p>无自定义 SQL，增删改查全部由 MyBatis-Plus {@code BaseMapper} 提供。登录时按手机号查单条再用
 * BCrypt 校验密码；角色字段取值见 {@code AdminRole}，停用账号不删行，由运营改状态。
 */
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
