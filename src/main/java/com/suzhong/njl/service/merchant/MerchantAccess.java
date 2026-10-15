package com.suzhong.njl.service.merchant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.web.ApiException;

@Component
public class MerchantAccess {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantOrgMapper merchantOrgMapper;

    public MerchantAccess(MerchantUserMapper merchantUserMapper, MerchantOrgMapper merchantOrgMapper) {
        this.merchantUserMapper = merchantUserMapper;
        this.merchantOrgMapper = merchantOrgMapper;
    }

    public long requireOrgId(AuthenticatedUser principal) {
        MerchantUser user = merchantUserMapper.selectById(principal.userId());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        if (principal.merchantOrgId() == null || !principal.merchantOrgId().equals(user.getMerchantOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        MerchantOrg org = merchantOrgMapper.selectById(user.getMerchantOrgId());
        if (org == null || org.getStatus() != MerchantOrgStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "商家已停用");
        }
        return user.getMerchantOrgId();
    }
}
