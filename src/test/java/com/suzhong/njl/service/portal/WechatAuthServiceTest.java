package com.suzhong.njl.service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.suzhong.njl.config.WechatMiniappProperties;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.PortalWechatIdentity;
import com.suzhong.njl.domain.enums.LoginChannel;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.PortalWechatIdentityMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.WechatAuthResponse;
import com.suzhong.njl.web.portal.dto.WechatBindRequest;
import com.suzhong.njl.web.portal.dto.WechatLoginRequest;
import com.suzhong.njl.wechat.WechatMiniappClient;
import com.suzhong.njl.wechat.WechatMiniappClient.JsCodeSession;

@ExtendWith(MockitoExtension.class)
class WechatAuthServiceTest {

    @Mock
    private PortalUserMapper portalUserMapper;
    @Mock
    private PortalWechatIdentityMapper identityMapper;
    @Mock
    private BookingIntentMapper bookingIntentMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private WechatMiniappClient wechatMiniappClient;

    private WechatMiniappProperties properties;
    private WechatAuthService service;

    @BeforeEach
    void setUp() {
        properties = new WechatMiniappProperties();
        properties.setAppId("wx-test-app");
        properties.setSecret("test-secret");
        service = new WechatAuthService(
                portalUserMapper,
                identityMapper,
                bookingIntentMapper,
                passwordEncoder,
                tokenStore,
                wechatMiniappClient,
                properties,
                transactionManager());
    }

    @Test
    void duplicateOpenidReusesTheCommittedUser() {
        PortalWechatIdentity existing = new PortalWechatIdentity();
        existing.setId(3L);
        existing.setPortalUserId(9L);
        existing.setAppId("wx-test-app");
        existing.setOpenid("openid-1");
        PortalUser owner = new PortalUser();
        owner.setId(9L);
        owner.setPhone("13900001111");
        owner.setPasswordHash("hash");
        owner.setDisplayName("网页用户");
        when(identityMapper.selectOne(any())).thenReturn(null, existing);
        when(portalUserMapper.insert(any(PortalUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PortalUser.class).setId(5L);
            return 1;
        });
        when(identityMapper.insert(any(PortalWechatIdentity.class))).thenThrow(new DuplicateKeyException("dup"));
        when(portalUserMapper.selectById(9L)).thenReturn(owner);
        when(identityMapper.selectCount(any())).thenReturn(1L);
        when(tokenStore.issue(any())).thenReturn(new IssuedToken("ab".repeat(32), Instant.parse("2026-10-18T04:00:00Z")));
        when(wechatMiniappClient.codeToSession("code-1")).thenReturn(new JsCodeSession("openid-1", "union-1"));

        WechatAuthResponse response = service.login(new WechatLoginRequest("code-1"));

        assertThat(response.userId()).isEqualTo(9L);
        assertThat(response.role()).isEqualTo(UserType.TOURIST);
        assertThat(response.wechatBound()).isTrue();
        assertThat(response.accountLinked()).isTrue();
        verify(identityMapper).updateById(any(PortalWechatIdentity.class));
        verify(tokenStore, never()).revokeWechatOpenid(any());
    }

    @Test
    void duplicateWithoutAVisibleRowIsRethrown() {
        when(identityMapper.selectOne(any())).thenReturn(null);
        when(portalUserMapper.insert(any(PortalUser.class))).thenReturn(1);
        when(identityMapper.insert(any(PortalWechatIdentity.class))).thenThrow(new DuplicateKeyException("dup"));
        when(wechatMiniappClient.codeToSession("code-1")).thenReturn(new JsCodeSession("openid-1", null));

        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code-1")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void blankCodeAndUnconfiguredDoNotCallWechat() {
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("  ")))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.CODE_REQUIRED.text());
        assertThatThrownBy(() -> service.login(null))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.CODE_REQUIRED.text());
        verifyNoInteractions(wechatMiniappClient);

        properties.setAppId(" ");
        properties.setSecret("");
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code-1")))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.NOT_CONFIGURED.text());
        verifyNoInteractions(wechatMiniappClient);
    }

    @Test
    void emptyOpenidIsUnavailable() {
        when(wechatMiniappClient.codeToSession("code-1")).thenReturn(new JsCodeSession("  ", null));
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code-1")))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());
        when(wechatMiniappClient.codeToSession("code-2")).thenReturn(null);
        assertThatThrownBy(() -> service.login(new WechatLoginRequest("code-2")))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.UNAVAILABLE.text());
    }

    @Test
    void bindDuplicateKeyStaysConflict() {
        AuthenticatedUser principal = new AuthenticatedUser(
                3L, UserType.TOURIST, null, null, WechatMessage.SHELL_DISPLAY_NAME, null, LoginChannel.WECHAT, "openid-x");
        when(portalUserMapper.selectById(3L)).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.bind(principal, new WechatBindRequest(null, "13900002222", "secret12")))
                .isInstanceOf(ApiException.class)
                .hasMessage(WechatMessage.PHONE_BOUND_OTHER_WECHAT.text());
    }

    private static PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
