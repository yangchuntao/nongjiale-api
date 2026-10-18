package com.suzhong.njl.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suzhong.njl.service.portal.HomeBannerService;
import com.suzhong.njl.web.portal.dto.HomeBannerItem;

@ExtendWith(MockitoExtension.class)
class HomeBannerControllerTest {

    @Mock
    private HomeBannerService homeBannerService;

    @InjectMocks
    private HomeBannerController controller;

    @Test
    void listReturnsThePortalCollection() {
        List<HomeBannerItem> items = List.of();
        when(homeBannerService.list()).thenReturn(items);

        assertThat(controller.list().code()).isZero();
        assertThat(controller.list().data()).isSameAs(items);
    }
}
