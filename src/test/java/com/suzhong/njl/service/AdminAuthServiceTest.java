package com.suzhong.njl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.entity.AdminUser;
import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.mapper.AdminUserMapper;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.AdminLoginRequest;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private TokenStore tokenStore;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void disabledOperatorIsForbiddenAfterPasswordMatches() {
        AdminUser user = new AdminUser();
        user.setId(2L);
        user.setPhone("13800000000");
        user.setPasswordHash("hash");
        user.setEnabled(false);
        user.setRole(AdminRole.OPERATOR);
        user.setDisplayName("停用运营");
        when(adminUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> adminAuthService.login(new AdminLoginRequest("13800000000", "SeedAdmin#2026")));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo("账号已停用");
    }

    @Test
    void unknownPhoneDoesNotIssueAToken() {
        when(adminUserMapper.selectOne(any())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> adminAuthService.login(new AdminLoginRequest("13900000000", "SeedAdmin#2026")));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
