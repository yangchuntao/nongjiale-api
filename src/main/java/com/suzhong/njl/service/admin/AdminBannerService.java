package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.BannerAdminResponse;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

@Service
public class AdminBannerService {

    private final HomeBannerMapper homeBannerMapper;
    private final RegionMapper regionMapper;
    private final AuditRecorder auditRecorder;

    public AdminBannerService(
            HomeBannerMapper homeBannerMapper,
            RegionMapper regionMapper,
            AuditRecorder auditRecorder) {
        this.homeBannerMapper = homeBannerMapper;
        this.regionMapper = regionMapper;
        this.auditRecorder = auditRecorder;
    }

    public PageResult<BannerAdminResponse> list(
            Boolean enabled,
            BannerScope scope,
            String keyword,
            Integer page,
            Integer pageSize) {
        String matched = BannerRules.keyword(keyword);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = homeBannerMapper.selectCount(filter(enabled, scope, matched));
        List<HomeBanner> rows = homeBannerMapper.selectList(filter(enabled, scope, matched)
                .orderByAsc(HomeBanner::getSortOrder)
                .orderByAsc(HomeBanner::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, Region> cities = citiesOf(rows);
        List<BannerAdminResponse> items = new ArrayList<>(rows.size());
        for (HomeBanner row : rows) {
            items.add(toResponse(row, cityOf(row, cities)));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public BannerAdminResponse detail(Long id) {
        return toResponse(requireBanner(id));
    }

    @Transactional
    public BannerAdminResponse create(long adminId, BannerWriteRequest request) {
        BannerRules.Prepared prepared = BannerRules.prepare(request, false);
        if (prepared.scope() == BannerScope.CITY) {
            requireOpenCity(prepared.cityId());
        }
        boolean enabled = BannerRules.enabled(request.enabled(), false);
        HomeBanner banner = new HomeBanner();
        apply(banner, prepared, enabled);
        banner.setCreatedBy(adminId);
        homeBannerMapper.insert(banner);
        auditRecorder.record(adminId, AuditAction.BANNER_CREATE, AuditTarget.HOME_BANNER, banner.getId(), detail(banner));
        return toResponse(requireBanner(banner.getId()));
    }

    @Transactional
    public BannerAdminResponse update(long adminId, Long id, BannerWriteRequest request) {
        BannerRules.Prepared prepared = BannerRules.prepare(request, true);
        if (prepared.scope() == BannerScope.CITY) {
            requireOpenCity(prepared.cityId());
        }
        boolean enabled = BannerRules.enabled(request.enabled(), true);
        HomeBanner banner = requireBanner(id);
        apply(banner, prepared, enabled);
        banner.setUpdatedBy(adminId);
        homeBannerMapper.updateById(banner);
        auditRecorder.record(adminId, AuditAction.BANNER_UPDATE, AuditTarget.HOME_BANNER, banner.getId(), detail(banner));
        return toResponse(requireBanner(id));
    }

    @Transactional
    public BannerAdminResponse setEnabled(long adminId, Long id, boolean enabled) {
        HomeBanner banner = requireBanner(id);
        if (Boolean.TRUE.equals(banner.getEnabled()) == enabled) {
            return toResponse(banner);
        }
        if (enabled && banner.getScope() == BannerScope.CITY && !cityCurrentlyOpen(banner.getCityId())) {
            throw BannerMessage.ENABLE_CITY_CLOSED.toException();
        }
        banner.setEnabled(enabled);
        banner.setUpdatedBy(adminId);
        homeBannerMapper.updateById(banner);
        AuditAction action = enabled ? AuditAction.BANNER_ENABLE : AuditAction.BANNER_DISABLE;
        auditRecorder.record(adminId, action, AuditTarget.HOME_BANNER, banner.getId(), detail(banner));
        return toResponse(requireBanner(id));
    }

    @Transactional
    public void delete(long adminId, Long id) {
        HomeBanner banner = requireBanner(id);
        if (homeBannerMapper.deleteById(id) != 1) {
            throw BannerMessage.NOT_FOUND.toException();
        }
        auditRecorder.record(adminId, AuditAction.BANNER_DELETE, AuditTarget.HOME_BANNER, id, detail(banner));
    }

    private void apply(HomeBanner banner, BannerRules.Prepared prepared, boolean enabled) {
        banner.setTitle(prepared.title());
        banner.setImageUrl(prepared.imageUrl());
        banner.setLinkUrl(prepared.linkUrl());
        banner.setScope(prepared.scope());
        banner.setCityId(prepared.scope() == BannerScope.ALL ? null : prepared.cityId());
        banner.setSortOrder(prepared.sortOrder());
        banner.setEnabled(enabled);
    }

    private Region requireOpenCity(Long cityId) {
        Region city = regionMapper.selectById(cityId);
        if (city == null || city.getLevel() != RegionLevel.CITY) {
            throw BannerMessage.CITY_NOT_FOUND.toException();
        }
        if (!Boolean.TRUE.equals(city.getEnabled())) {
            throw BannerMessage.REGION_DISABLED.toException();
        }
        if (city.getOpenStatus() != RegionOpenStatus.OPEN) {
            throw BannerMessage.CITY_NOT_OPEN.toException();
        }
        return city;
    }

    private boolean cityCurrentlyOpen(Long cityId) {
        if (cityId == null) {
            return false;
        }
        return CityRules.isOpen(regionMapper.selectById(cityId));
    }

    private LambdaQueryWrapper<HomeBanner> filter(Boolean enabled, BannerScope scope, String keyword) {
        LambdaQueryWrapper<HomeBanner> query = Wrappers.lambdaQuery();
        if (enabled != null) {
            query.eq(HomeBanner::getEnabled, enabled);
        }
        if (scope != null) {
            query.eq(HomeBanner::getScope, scope);
        }
        if (keyword != null) {
            query.and(nested -> nested.like(HomeBanner::getTitle, keyword).or().like(HomeBanner::getLinkUrl, keyword));
        }
        return query;
    }

    private HomeBanner requireBanner(Long id) {
        HomeBanner banner = homeBannerMapper.selectById(id);
        if (banner == null) {
            throw BannerMessage.NOT_FOUND.toException();
        }
        return banner;
    }

    private Map<Long, Region> citiesOf(List<HomeBanner> rows) {
        Set<Long> ids = new LinkedHashSet<>();
        for (HomeBanner row : rows) {
            if (row.getScope() == BannerScope.CITY && row.getCityId() != null) {
                ids.add(row.getCityId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery().in(Region::getId, ids));
        Map<Long, Region> byId = new HashMap<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }

    private Region cityOf(HomeBanner banner, Map<Long, Region> cities) {
        if (banner.getScope() != BannerScope.CITY || banner.getCityId() == null) {
            return null;
        }
        return cities.get(banner.getCityId());
    }

    private BannerAdminResponse toResponse(HomeBanner banner) {
        Region city = null;
        if (banner.getScope() == BannerScope.CITY && banner.getCityId() != null) {
            city = regionMapper.selectById(banner.getCityId());
        }
        return toResponse(banner, city);
    }

    private BannerAdminResponse toResponse(HomeBanner banner, Region city) {
        boolean cityScope = banner.getScope() == BannerScope.CITY;
        return new BannerAdminResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getScope(),
                cityScope ? banner.getCityId() : null,
                cityScope && city != null ? city.getSlug() : null,
                cityScope && city != null ? city.getName() : null,
                cityScope ? CityRules.isOpen(city) : null,
                banner.getSortOrder(),
                banner.getEnabled(),
                banner.getCreatedAt(),
                banner.getUpdatedAt());
    }

    private Map<String, Object> detail(HomeBanner banner) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("scope", banner.getScope() == null ? null : banner.getScope().code());
        detail.put("cityId", banner.getCityId());
        detail.put("enabled", banner.getEnabled());
        detail.put("sortOrder", banner.getSortOrder());
        return detail;
    }
}
