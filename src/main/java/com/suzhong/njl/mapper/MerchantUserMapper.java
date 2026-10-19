package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.MerchantUser;
/**
 * 商家登录账号 Mapper，对应 {@code merchant_user} 表。
 *
 * <p>无自定义 SQL。账号挂在商家主体下，登录按手机号查单条并校验 BCrypt 密码；
 * 主体或账号停用返回 403，令牌角色固定为 {@code MERCHANT}。
 */
public interface MerchantUserMapper extends BaseMapper<MerchantUser> {
}
