package com.suzhong.njl.service.merchant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.merchant.dto.MerchantLoginRequest;
import com.suzhong.njl.web.merchant.dto.MerchantLoginResponse;

@Service
public class MerchantAuthService {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantOrgMapper merchantOrgMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public MerchantAuthService(
            MerchantUserMapper merchantUserMapper,
            MerchantOrgMapper merchantOrgMapper,
            PasswordEncoder passwordEncoder,
            TokenStore tokenStore) {
        this.merchantUserMapper = merchantUserMapper;
        this.merchantOrgMapper = merchantOrgMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    public MerchantLoginResponse login(MerchantLoginRequest request) {
        MerchantUser user = merchantUserMapper.selectOne(
                Wrappers.<MerchantUser>lambdaQuery().eq(MerchantUser::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        MerchantOrg org = merchantOrgMapper.selectById(user.getMerchantOrgId());
        if (org == null || org.getStatus() != MerchantOrgStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "商家已停用");
        }
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.MERCHANT,
                null,
                user.getMerchantOrgId(),
                user.getDisplayName(),
                user.getPhone());
        IssuedToken issued = tokenStore.issue(principal);
        return new MerchantLoginResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                user.getMerchantOrgId(),
                UserType.MERCHANT,
                user.getDisplayName());
    }
}
