package com.suzhong.njl.service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suzhong.njl.domain.entity.HomeBanner;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.mapper.HomeBannerMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.portal.dto.HomeBannerItem;

@ExtendWith(MockitoExtension.class)
class HomeBannerServiceTest {

    @Mock
    private HomeBannerMapper homeBannerMapper;

    @Mock
    private RegionMapper regionMapper;

    private HomeBannerService service;

    @BeforeEach
    void setUp() {
        service = new HomeBannerService(homeBannerMapper, regionMapper);
    }

    @Test
    void keepsAllAndOpenCitiesInQueryOrder() {
        HomeBanner all = banner(1L, BannerScope.ALL, 9L, true, 10);
        HomeBanner open = banner(2L, BannerScope.CITY, 8L, true, 20);
        HomeBanner closed = banner(3L, BannerScope.CITY, 7L, true, 30);
        HomeBanner disabled = banner(4L, BannerScope.ALL, null, false, 5);
        HomeBanner blankScope = banner(5L, null, null, true, 1);
        HomeBanner missing = banner(6L, BannerScope.CITY, null, true, 40);
        when(homeBannerMapper.selectList(any())).thenReturn(List.of(all, open, closed, disabled, blankScope, missing));
        when(regionMapper.selectList(any())).thenReturn(List.of(region(8L, RegionOpenStatus.OPEN), region(7L, RegionOpenStatus.CLOSED)));

        List<HomeBannerItem> items = service.list();

        assertThat(items).extracting(HomeBannerItem::id).containsExactly(1L, 2L);
        assertThat(items.get(0).cityId()).isNull();
        assertThat(items.get(0).citySlug()).isNull();
        assertThat(items.get(0).enabled()).isTrue();
        assertThat(items.get(1).citySlug()).isEqualTo("yangzhou");
        assertThat(items.get(1).cityName()).isEqualTo("扬州市");
        assertThat(items.get(1).enabled()).isTrue();
    }

    @Test
    void skipsCityLookupWhenEveryRowIsSiteWide() {
        when(homeBannerMapper.selectList(any())).thenReturn(List.of(banner(1L, BannerScope.ALL, null, true, 10)));

        assertThat(service.list()).hasSize(1);
        verify(regionMapper, never()).selectList(any());
    }

    private static HomeBanner banner(Long id, BannerScope scope, Long cityId, boolean enabled, int sortOrder) {
        HomeBanner banner = new HomeBanner();
        banner.setId(id);
        banner.setTitle("标题" + id);
        banner.setImageUrl("https://img.example/" + id + ".png");
        banner.setLinkUrl("/");
        banner.setScope(scope);
        banner.setCityId(cityId);
        banner.setEnabled(enabled);
        banner.setSortOrder(sortOrder);
        return banner;
    }

    private static Region region(long id, RegionOpenStatus status) {
        Region region = new Region();
        region.setId(id);
        region.setLevel(RegionLevel.CITY);
        region.setEnabled(true);
        region.setOpenStatus(status);
        region.setSlug("yangzhou");
        region.setName("扬州市");
        return region;
    }
}
