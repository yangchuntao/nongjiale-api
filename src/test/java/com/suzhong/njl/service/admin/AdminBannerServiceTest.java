package com.suzhong.njl.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.entity.HomeBanner;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.BannerMessage;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.mapper.HomeBannerMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.BannerAdminResponse;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

@ExtendWith(MockitoExtension.class)
class AdminBannerServiceTest {

    @Mock
    private HomeBannerMapper homeBannerMapper;

    @Mock
    private RegionMapper regionMapper;

    @Mock
    private AuditRecorder auditRecorder;

    private AdminBannerService service;

    @BeforeEach
    void setUp() {
        service = new AdminBannerService(homeBannerMapper, regionMapper, auditRecorder);
    }

    @Test
    void listMapsOpenCityAndTreatsAMissingCountAsZero() {
        HomeBanner all = banner(1L, BannerScope.ALL, null, true, 10);
        HomeBanner city = banner(2L, BannerScope.CITY, 8L, false, 20);
        HomeBanner missingCity = banner(3L, BannerScope.CITY, null, true, 30);
        when(homeBannerMapper.selectCount(any())).thenReturn(null);
        when(homeBannerMapper.selectList(any())).thenReturn(List.of(all, city, missingCity));
        when(regionMapper.selectList(any())).thenReturn(List.of(city(8L, true, RegionOpenStatus.OPEN)));

        PageResult<BannerAdminResponse> page = service.list(Boolean.FALSE, BannerScope.CITY, "扬州", 2, 10);

        assertThat(page.total()).isZero();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).hasSize(3);
        assertThat(page.items().get(0).cityId()).isNull();
        assertThat(page.items().get(0).cityOpen()).isNull();
        assertThat(page.items().get(1).citySlug()).isEqualTo("yangzhou");
        assertThat(page.items().get(1).cityName()).isEqualTo("扬州市");
        assertThat(page.items().get(1).cityOpen()).isTrue();
        assertThat(page.items().get(2).cityOpen()).isFalse();
    }

    @Test
    void listWithoutFiltersDoesNotLoadCities() {
        when(homeBannerMapper.selectCount(any())).thenReturn(0L);
        when(homeBannerMapper.selectList(any())).thenReturn(List.of());

        PageResult<BannerAdminResponse> page = service.list(null, null, "  ", null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
        verify(regionMapper, never()).selectList(any());
    }

    @Test
    void keywordTooLongDoesNotQuery() {
        assertThatThrownBy(() -> service.list(null, null, "k".repeat(65), 1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessage("参数不正确");
        verify(homeBannerMapper, never()).selectCount(any());
    }

    @Test
    void createRejectsAClosedCityBeforeEnabledAndAuditsAnOpenCity() {
        when(regionMapper.selectById(8L)).thenReturn(city(8L, true, RegionOpenStatus.CLOSED));
        BannerWriteRequest closed = write(BannerScope.CITY, 8L, null, null);
        assertThatThrownBy(() -> service.create(3L, closed))
                .isInstanceOf(ApiException.class)
                .hasMessage(BannerMessage.CITY_NOT_OPEN.text());
        verify(homeBannerMapper, never()).insert(any(HomeBanner.class));

        when(regionMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.create(3L, write(BannerScope.CITY, 9L, Boolean.FALSE, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessage(BannerMessage.CITY_NOT_FOUND.text());

        Region district = city(10L, true, RegionOpenStatus.OPEN);
        district.setLevel(RegionLevel.DISTRICT);
        when(regionMapper.selectById(10L)).thenReturn(district);
        assertThatThrownBy(() -> service.create(3L, write(BannerScope.CITY, 10L, Boolean.TRUE, 1)))
                .hasMessage(BannerMessage.CITY_NOT_FOUND.text());

        when(regionMapper.selectById(11L)).thenReturn(city(11L, false, RegionOpenStatus.OPEN));
        assertThatThrownBy(() -> service.create(3L, write(BannerScope.CITY, 11L, Boolean.TRUE, 1)))
                .hasMessage(BannerMessage.REGION_DISABLED.text());

        Region unopened = city(12L, true, null);
        when(regionMapper.selectById(12L)).thenReturn(unopened);
        assertThatThrownBy(() -> service.create(3L, write(BannerScope.CITY, 12L, Boolean.TRUE, 1)))
                .hasMessage(BannerMessage.CITY_NOT_OPEN.text());

        when(regionMapper.selectById(8L)).thenReturn(city(8L, true, RegionOpenStatus.OPEN));
        AtomicReference<HomeBanner> stored = new AtomicReference<>();
        when(homeBannerMapper.insert(any(HomeBanner.class))).thenAnswer(invocation -> {
            HomeBanner row = invocation.getArgument(0);
            row.setId(21L);
            row.setCreatedAt(LocalDateTime.of(2026, 10, 18, 9, 0));
            stored.set(row);
            return 1;
        });
        when(homeBannerMapper.selectById(21L)).thenAnswer(invocation -> stored.get());

        BannerAdminResponse created = service.create(3L, write(BannerScope.CITY, 8L, null, null));

        assertThat(created.id()).isEqualTo(21L);
        assertThat(created.enabled()).isTrue();
        assertThat(created.sortOrder()).isEqualTo(100);
        assertThat(created.cityOpen()).isTrue();
        assertThat(stored.get().getCreatedBy()).isEqualTo(3L);
        verify(auditRecorder).record(eq(3L), eq(AuditAction.BANNER_CREATE), eq(AuditTarget.HOME_BANNER), eq(21L), any());
    }

    @Test
    void updateClearsCityWhenSwitchingToAllAndRequiresTheBanner() {
        when(homeBannerMapper.selectById(4L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(4L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        HomeBanner existing = banner(4L, BannerScope.CITY, 8L, true, 20);
        when(homeBannerMapper.selectById(4L)).thenReturn(existing);
        when(homeBannerMapper.updateById(any(HomeBanner.class))).thenReturn(1);

        BannerAdminResponse updated = service.update(3L, 4L, write(BannerScope.ALL, null, Boolean.FALSE, 0));

        assertThat(updated.scope()).isEqualTo(BannerScope.ALL);
        assertThat(updated.cityId()).isNull();
        assertThat(updated.enabled()).isFalse();
        assertThat(existing.getCityId()).isNull();
        assertThat(existing.getUpdatedBy()).isEqualTo(3L);
        verify(auditRecorder).record(eq(3L), eq(AuditAction.BANNER_UPDATE), eq(AuditTarget.HOME_BANNER), eq(4L), any());

        when(regionMapper.selectById(8L)).thenReturn(city(8L, true, RegionOpenStatus.OPEN));
        BannerAdminResponse moved = service.update(3L, 4L, write(BannerScope.CITY, 8L, Boolean.TRUE, 4));
        assertThat(moved.scope()).isEqualTo(BannerScope.CITY);
        assertThat(moved.cityId()).isEqualTo(8L);
        assertThat(moved.cityOpen()).isTrue();
        assertThat(existing.getCityId()).isEqualTo(8L);
    }

    @Test
    void enableIsIdempotentAndConflictsWhenTheCityIsNoLongerOpen() {
        HomeBanner enabled = banner(5L, BannerScope.CITY, 8L, true, 10);
        when(homeBannerMapper.selectById(5L)).thenReturn(enabled);
        BannerAdminResponse same = service.setEnabled(3L, 5L, true);
        assertThat(same.enabled()).isTrue();
        verify(homeBannerMapper, never()).updateById(any(HomeBanner.class));
        verify(auditRecorder, never()).record(any(), any(), any(), any(), any());

        HomeBanner stopped = banner(6L, BannerScope.CITY, null, false, 10);
        when(homeBannerMapper.selectById(6L)).thenReturn(stopped);
        assertThatThrownBy(() -> service.setEnabled(3L, 6L, true))
                .hasMessage(BannerMessage.ENABLE_CITY_CLOSED.text());

        HomeBanner closed = banner(7L, BannerScope.CITY, 8L, false, 10);
        when(homeBannerMapper.selectById(7L)).thenReturn(closed);
        when(regionMapper.selectById(8L)).thenReturn(city(8L, true, RegionOpenStatus.CLOSED));
        assertThatThrownBy(() -> service.setEnabled(3L, 7L, true))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(auditRecorder, never()).record(any(), eq(AuditAction.BANNER_ENABLE), any(), any(), any());

        when(homeBannerMapper.updateById(any(HomeBanner.class))).thenReturn(1);
        BannerAdminResponse disabled = service.setEnabled(3L, 5L, false);
        assertThat(disabled.enabled()).isFalse();
        verify(auditRecorder).record(eq(3L), eq(AuditAction.BANNER_DISABLE), eq(AuditTarget.HOME_BANNER), eq(5L), any());

        HomeBanner plain = banner(9L, BannerScope.ALL, null, false, 1);
        when(homeBannerMapper.selectById(9L)).thenReturn(plain);
        assertThat(service.setEnabled(3L, 9L, true).enabled()).isTrue();
        verify(auditRecorder).record(eq(3L), eq(AuditAction.BANNER_ENABLE), eq(AuditTarget.HOME_BANNER), eq(9L), any());
    }

    @Test
    void deleteAuditsAndRejectsAMissingRow() {
        when(homeBannerMapper.selectById(4L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(3L, 4L)).hasMessage(BannerMessage.NOT_FOUND.text());

        HomeBanner row = banner(4L, null, null, true, 1);
        when(homeBannerMapper.selectById(4L)).thenReturn(row);
        when(homeBannerMapper.deleteById(4L)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(3L, 4L)).hasMessage(BannerMessage.NOT_FOUND.text());
        verify(auditRecorder, never()).record(any(), any(), any(), any(), any());

        when(homeBannerMapper.deleteById(4L)).thenReturn(1);
        service.delete(3L, 4L);
        verify(auditRecorder).record(eq(3L), eq(AuditAction.BANNER_DELETE), eq(AuditTarget.HOME_BANNER), eq(4L), any());
    }

    private static BannerWriteRequest write(BannerScope scope, Long cityId, Boolean enabled, Integer sortOrder) {
        return new BannerWriteRequest("标题", "https://img.example/a.png", "/", scope, cityId, sortOrder, enabled);
    }

    private static HomeBanner banner(Long id, BannerScope scope, Long cityId, boolean enabled, int sortOrder) {
        HomeBanner banner = new HomeBanner();
        banner.setId(id);
        banner.setTitle("标题");
        banner.setImageUrl("https://img.example/a.png");
        banner.setLinkUrl("/");
        banner.setScope(scope);
        banner.setCityId(cityId);
        banner.setEnabled(enabled);
        banner.setSortOrder(sortOrder);
        banner.setCreatedAt(LocalDateTime.of(2026, 10, 18, 8, 0));
        banner.setUpdatedAt(LocalDateTime.of(2026, 10, 18, 8, 0));
        return banner;
    }

    private static Region city(long id, boolean enabled, RegionOpenStatus status) {
        Region region = new Region();
        region.setId(id);
        region.setLevel(RegionLevel.CITY);
        region.setEnabled(enabled);
        region.setOpenStatus(status);
        region.setSlug("yangzhou");
        region.setName("扬州市");
        return region;
    }
}
