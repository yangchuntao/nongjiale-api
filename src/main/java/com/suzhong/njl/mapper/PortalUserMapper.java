package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.PortalUser;
/**
 * 游客账号 Mapper，对应 {@code portal_user} 表。
 *
 * <p>无自定义 SQL。微信首次登录会按 openid 建号，此时手机号可为空；手机号为空时提交预订意向
 * 返回 403 提示先绑定，而不是插入空联系方式的数据。
 */
public interface PortalUserMapper extends BaseMapper<PortalUser> {
}
