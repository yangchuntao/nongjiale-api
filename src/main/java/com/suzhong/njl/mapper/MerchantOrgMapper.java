package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.MerchantOrg;
/**
 * 商家主体 Mapper，对应 {@code merchant_org} 表。
 *
 * <p>无自定义 SQL。主体是门店与商家账号的归属单位；停用主体后其旧令牌失效，
 * 门店数据保留不级联删除。
 */
public interface MerchantOrgMapper extends BaseMapper<MerchantOrg> {
}
