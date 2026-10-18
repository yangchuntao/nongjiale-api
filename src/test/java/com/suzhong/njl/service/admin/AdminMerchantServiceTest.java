package com.suzhong.njl.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminMerchantResponse;
import com.suzhong.njl.web.admin.dto.MerchantStatusPatchRequest;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.ResetPasswordResponse;

@ExtendWith(MockitoExtension.class)
class AdminMerchantServiceTest {

    @Mock
    private MerchantOrgMapper merchantOrgMapper;

    @Mock
    private MerchantUserMapper merchantUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private AuditRecorder auditRecorder;

    private AdminMerchantService service;

    @BeforeEach
    void setUp() {
        service = new AdminMerchantService(
                merchantOrgMapper, merchantUserMapper, passwordEncoder, tokenStore, auditRecorder);
    }

    @Test
    void listGroupsAccountsAndTreatsAMissingCountAsZero() {
        when(merchantOrgMapper.selectCount(any())).thenReturn(null);
        when(merchantOrgMapper.selectList(any())).thenReturn(List.of());
        PageResult<AdminMerchantResponse> empty = service.list(null, 1, 20);
        assertThat(empty.total()).isZero();
        assertThat(empty.items()).isEmpty();
        verify(merchantUserMapper, never()).selectList(any());

        MerchantOrg org = org(4L, MerchantOrgStatus.ACTIVE);
        MerchantUser user = user(8L, 4L, true);
        when(merchantOrgMapper.selectCount(any())).thenReturn(1L);
        when(merchantOrgMapper.selectList(any())).thenReturn(List.of(org));
        when(merchantUserMapper.selectList(any())).thenReturn(List.of(user));

        PageResult<AdminMerchantResponse> page = service.list(MerchantOrgStatus.ACTIVE, 1, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).name()).isEqualTo("表弟店");
        assertThat(page.items().get(0).users()).singleElement().satisfies(item -> {
            assertThat(item.phone()).isEqualTo("17606838921");
            assertThat(item.enabled()).isTrue();
        });
    }

    @Test
    void statusChangeRequiresAFieldAndRevokesTokensWhenDisabling() {
        assertThatThrownBy(() -> service.updateStatus(1L, 4L, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("请指定启停状态");
        assertThatThrownBy(() -> service.updateStatus(1L, 4L, new MerchantStatusPatchRequest(null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        when(merchantOrgMapper.selectById(4L)).thenReturn(null);
        assertThatThrownBy(() -> service.updateStatus(1L, 4L, new MerchantStatusPatchRequest(MerchantOrgStatus.DISABLED, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("商家不存在");

        MerchantOrg org = org(4L, MerchantOrgStatus.ACTIVE);
        MerchantUser user = user(8L, 4L, true);
        when(merchantOrgMapper.selectById(4L)).thenReturn(org);
        when(merchantUserMapper.selectList(any())).thenReturn(List.of(user));

        AdminMerchantResponse disabled = service.updateStatus(
                1L, 4L, new MerchantStatusPatchRequest(MerchantOrgStatus.DISABLED, false));

        assertThat(disabled.status()).isEqualTo(MerchantOrgStatus.DISABLED);
        assertThat(user.getEnabled()).isFalse();
        verify(tokenStore).revokeMerchantOrg(4L);
        verify(auditRecorder).record(eq(1L), eq(AuditAction.MERCHANT_DISABLE), eq(AuditTarget.MERCHANT_ORG), eq(4L), any());

        AdminMerchantResponse enabled = service.updateStatus(
                1L, 4L, new MerchantStatusPatchRequest(MerchantOrgStatus.ACTIVE, true));
        assertThat(enabled.status()).isEqualTo(MerchantOrgStatus.ACTIVE);
        assertThat(user.getEnabled()).isTrue();
        verify(auditRecorder).record(eq(1L), eq(AuditAction.MERCHANT_ENABLE), eq(AuditTarget.MERCHANT_ORG), eq(4L), any());
    }

    @Test
    void resetPasswordRejectsAnOrgWithoutAccountsAndKeepsASuppliedPassword() {
        when(merchantOrgMapper.selectById(4L)).thenReturn(org(4L, MerchantOrgStatus.ACTIVE));
        when(merchantUserMapper.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.resetPassword(4L, new PlainPasswordRequest("merchant12")))
                .isInstanceOf(ApiException.class)
                .hasMessage("商家账号不存在");

        MerchantUser later = user(9L, 4L, true);
        later.setPhone("13900000002");
        MerchantUser earlier = user(8L, 4L, true);
        when(merchantUserMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(later, earlier)));
        when(passwordEncoder.encode("merchant12")).thenReturn("hash");

        ResetPasswordResponse response = service.resetPassword(4L, new PlainPasswordRequest("merchant12"));

        assertThat(response.merchantOrgId()).isEqualTo(4L);
        assertThat(response.merchantUserId()).isEqualTo(8L);
        assertThat(response.phone()).isEqualTo("17606838921");
        assertThat(response.plainPassword()).isEqualTo("merchant12");
        assertThat(earlier.getPasswordHash()).isEqualTo("hash");
        assertThat(later.getPasswordHash()).isEqualTo("hash");
        verify(tokenStore).revokeMerchantOrg(4L);
    }

    @Test
    void resetPasswordGeneratesAPasswordWhenNoneIsSupplied() {
        when(merchantOrgMapper.selectById(4L)).thenReturn(org(4L, MerchantOrgStatus.ACTIVE));
        when(merchantUserMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(user(8L, 4L, true))));
        when(passwordEncoder.encode(any())).thenReturn("generated-hash");

        ResetPasswordResponse response = service.resetPassword(4L, null);

        assertThat(response.plainPassword()).hasSize(10);
        assertThat(response.merchantUserId()).isEqualTo(8L);
    }

    private static MerchantOrg org(Long id, MerchantOrgStatus status) {
        MerchantOrg org = new MerchantOrg();
        org.setId(id);
        org.setName("表弟店");
        org.setContactName("表弟");
        org.setContactPhone("13900000000");
        org.setStatus(status);
        return org;
    }

    private static MerchantUser user(Long id, Long orgId, boolean enabled) {
        MerchantUser user = new MerchantUser();
        user.setId(id);
        user.setMerchantOrgId(orgId);
        user.setPhone("17606838921");
        user.setDisplayName("店主");
        user.setEnabled(enabled);
        return user;
    }
}
