package com.suzhong.njl.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.AdminUser;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.AdminUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.AdminLoginRequest;
import com.suzhong.njl.web.admin.dto.AdminLoginResponse;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public AdminAuthService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder, TokenStore tokenStore) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.OPERATOR,
                user.getRole(),
                null,
                user.getDisplayName(),
                user.getPhone());
        IssuedToken issued = tokenStore.issue(principal);
        return new AdminLoginResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                user.getRole(),
                user.getDisplayName());
    }
}
