package com.suzhong.njl.service.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.web.ApiException;

@ExtendWith(MockitoExtension.class)
class MerchantAccessTest {

    @Mock
    private MerchantUserMapper merchantUserMapper;

    @Mock
    private MerchantOrgMapper merchantOrgMapper;

    @InjectMocks
    private MerchantAccess merchantAccess;

    @Test
    void activeAccountReturnsItsOrg() {
        MerchantUser user = user(true, 9L);
        MerchantOrg org = new MerchantOrg();
        org.setStatus(MerchantOrgStatus.ACTIVE);
        when(merchantUserMapper.selectById(5L)).thenReturn(user);
        when(merchantOrgMapper.selectById(9L)).thenReturn(org);

        assertThat(merchantAccess.requireOrgId(principal(9L))).isEqualTo(9L);
    }

    @Test
    void missingOrDisabledIdentitiesAreRejected() {
        when(merchantUserMapper.selectById(5L)).thenReturn(null);
        assertThatThrownBy(() -> merchantAccess.requireOrgId(principal(9L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        when(merchantUserMapper.selectById(5L)).thenReturn(user(false, 9L));
        assertForbidden("账号已停用", principal(9L));

        when(merchantUserMapper.selectById(5L)).thenReturn(user(null, 9L));
        assertForbidden("账号已停用", principal(9L));

        when(merchantUserMapper.selectById(5L)).thenReturn(user(true, 9L));
        assertForbidden("无权限", principal(null));
        assertForbidden("无权限", principal(8L));

        when(merchantOrgMapper.selectById(9L)).thenReturn(null);
        assertForbidden("商家已停用", principal(9L));

        MerchantOrg disabled = new MerchantOrg();
        disabled.setStatus(MerchantOrgStatus.DISABLED);
        when(merchantOrgMapper.selectById(9L)).thenReturn(disabled);
        assertForbidden("商家已停用", principal(9L));
    }

    private void assertForbidden(String message, AuthenticatedUser principal) {
        assertThatThrownBy(() -> merchantAccess.requireOrgId(principal))
                .isInstanceOf(ApiException.class)
                .hasMessage(message);
    }

    private static AuthenticatedUser principal(Long orgId) {
        return new AuthenticatedUser(5L, UserType.MERCHANT, null, orgId, "商家", "13800000000");
    }

    private static MerchantUser user(Boolean enabled, Long orgId) {
        MerchantUser user = new MerchantUser();
        user.setId(5L);
        user.setEnabled(enabled);
        user.setMerchantOrgId(orgId);
        return user;
    }
}
