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
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.admin.AdminArticleService;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ArticleAdminResponse;
import com.suzhong.njl.web.admin.dto.ArticleWriteRequest;

@ExtendWith(MockitoExtension.class)
class AdminArticleControllerTest {

    @Mock
    private AdminArticleService adminArticleService;

    @InjectMocks
    private AdminArticleController controller;

    @Test
    void listDetailUpdateAndDeleteDelegate() {
        PageResult<ArticleAdminResponse> page = new PageResult<>(List.of(), 0, 1, 20);
        ArticleAdminResponse detail = mock(ArticleAdminResponse.class);
        ArticleWriteRequest request = new ArticleWriteRequest("标题", null, null, "正文", ArticleStatus.DRAFT, null);
        when(adminArticleService.list(ArticleStatus.PUBLISHED, 2, 10)).thenReturn(page);
        when(adminArticleService.detail(4L)).thenReturn(detail);
        when(adminArticleService.update(4L, request)).thenReturn(detail);

        assertThat(controller.list(ArticleStatus.PUBLISHED, 2, 10).data()).isSameAs(page);
        assertThat(controller.detail(4L).data()).isSameAs(detail);
        assertThat(controller.update(4L, request).data()).isSameAs(detail);
        assertThat(controller.delete(4L).data()).containsEntry("id", 4L);
        verify(adminArticleService).delete(4L);
    }

    @Test
    void createUsesTheOperatorId() {
        ArticleWriteRequest request = new ArticleWriteRequest("标题", null, null, null, ArticleStatus.DRAFT, null);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(
                new AuthenticatedUser(7L, UserType.OPERATOR, AdminRole.OPERATOR, null, "运营", "17606838920"));
        ArticleAdminResponse created = mock(ArticleAdminResponse.class);
        when(adminArticleService.create(7L, request)).thenReturn(created);

        assertThat(controller.create(authentication, request).data()).isSameAs(created);
    }
}
