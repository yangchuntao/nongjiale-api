package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.MerchantApply;
/**
 * 商家入驻申请 Mapper，对应 {@code merchant_apply} 表。
 *
 * <p>无自定义 SQL。运营按状态筛选待审队列；通过时开通主体与账号并回写申请状态，
 * 驳回必须带原因且仅待审状态可操作，状态判断在 Service 层完成。
 */
public interface MerchantApplyMapper extends BaseMapper<MerchantApply> {
}
