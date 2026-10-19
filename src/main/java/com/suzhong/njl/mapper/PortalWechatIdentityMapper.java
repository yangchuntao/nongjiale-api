package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.PortalWechatIdentity;

/**
 * 游客微信身份 Mapper，对应 {@code portal_wechat_identity} 表。
 *
 * <p>无自定义 SQL。记录 AppID + openid 与游客账号的绑定关系，同一 openid 再次登录复用原账号；
 * 解绑只删当前 AppID 的记录，不影响网页版密码登录。
 */
public interface PortalWechatIdentityMapper extends BaseMapper<PortalWechatIdentity> {
}
