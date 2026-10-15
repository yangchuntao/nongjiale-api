package com.suzhong.njl.service.portal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.service.support.PhoneMask;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.PortalAuthResponse;
import com.suzhong.njl.web.portal.dto.PortalLoginRequest;
import com.suzhong.njl.web.portal.dto.PortalMeResponse;
import com.suzhong.njl.web.portal.dto.PortalRegisterRequest;

@Service
public class PortalAuthService {

    private final PortalUserMapper portalUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public PortalAuthService(PortalUserMapper portalUserMapper, PasswordEncoder passwordEncoder, TokenStore tokenStore) {
        this.portalUserMapper = portalUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    public PortalAuthResponse register(PortalRegisterRequest request) {
        PortalUser existing = portalUserMapper.selectOne(
                Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, request.phone()));
        if (existing != null) {
            throw new ApiException(HttpStatus.CONFLICT, "手机号已注册");
        }
        PortalUser user = new PortalUser();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = PhoneMask.mask(request.phone());
        }
        user.setDisplayName(displayName);
        try {
            portalUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "手机号已注册");
        }
        return issue(user);
    }

    public PortalAuthResponse login(PortalLoginRequest request) {
        PortalUser user = portalUserMapper.selectOne(
                Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        return issue(user);
    }

    public PortalMeResponse me(AuthenticatedUser user) {
        PortalUser stored = portalUserMapper.selectById(user.userId());
        if (stored == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return new PortalMeResponse(stored.getId(), stored.getPhone(), stored.getDisplayName(), UserType.TOURIST);
    }

    private PortalAuthResponse issue(PortalUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.TOURIST,
                null,
                null,
                user.getDisplayName(),
                user.getPhone());
        IssuedToken issued = tokenStore.issue(principal);
        return new PortalAuthResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                UserType.TOURIST,
                user.getDisplayName());
    }
}
