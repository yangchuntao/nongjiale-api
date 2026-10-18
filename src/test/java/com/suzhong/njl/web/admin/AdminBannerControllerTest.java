package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.admin.AdminBannerService;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.BannerAdminResponse;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

@ExtendWith(MockitoExtension.class)
class AdminBannerControllerTest {

    @Mock
    private AdminBannerService adminBannerService;

    @InjectMocks
    private AdminBannerController controller;

    @Test
    void listDetailAndMutationsDelegateToTheOperator() {
        PageResult<BannerAdminResponse> page = new PageResult<>(List.of(), 0, 1, 20);
        BannerAdminResponse detail = mock(BannerAdminResponse.class);
        BannerWriteRequest request = new BannerWriteRequest(null, "https://img.example/a.png", "/", BannerScope.ALL, null, 10, true);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(
                new AuthenticatedUser(7L, UserType.OPERATOR, AdminRole.AGENT, null, "客服", "13800000081"));
        when(adminBannerService.list(Boolean.TRUE, BannerScope.ALL, "扬州", 2, 10)).thenReturn(page);
        when(adminBannerService.detail(4L)).thenReturn(detail);
        when(adminBannerService.create(7L, request)).thenReturn(detail);
        when(adminBannerService.update(7L, 4L, request)).thenReturn(detail);
        when(adminBannerService.setEnabled(7L, 4L, true)).thenReturn(detail);
        when(adminBannerService.setEnabled(7L, 4L, false)).thenReturn(detail);

        assertThat(controller.list(Boolean.TRUE, BannerScope.ALL, "扬州", 2, 10).data()).isSameAs(page);
        assertThat(controller.detail(4L).data()).isSameAs(detail);
        assertThat(controller.create(authentication, request).data()).isSameAs(detail);
        assertThat(controller.update(authentication, 4L, request).data()).isSameAs(detail);
        assertThat(controller.enable(authentication, 4L).data()).isSameAs(detail);
        assertThat(controller.disable(authentication, 4L).data()).isSameAs(detail);
        assertThat(controller.delete(authentication, 4L).data()).containsEntry("id", 4L);
        verify(adminBannerService).delete(7L, 4L);
    }
}
