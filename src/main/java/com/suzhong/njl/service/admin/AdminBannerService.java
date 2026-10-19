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

/**
 * 运营端首页 Banner 管理服务（{@code /api/admin/banners}）。
 *
 * <p>职责：Banner 的分页查询、详情、新建、全量更新、行内启停与物理删除。投放范围只有两种：
 * {@code ALL} 全站、{@code CITY} 指定市；指定市必须是启用且已开城的市。
 *
 * <p>事务边界：所有写操作各自一个事务，业务落库与审计写入同事务；查询不开事务。
 *
 * <p>权限：本类不做角色校验，{@code /api/admin/**} 的 Bearer 与 {@code OPERATOR}/{@code AGENT}
 * 由安全配置和控制器保证。
 *
 * <p>规则入口：字段校验、排序与投放范围互斥由 {@link BannerRules} 统一处理；
 * 市的开通态判断走 {@link CityRules#isOpen}，与门户口径保持一致。
 *
 * <p>依赖：{@link AuditRecorder} 记录新建、更新、启停、删除，审计写失败会随事务一起失败。
 */
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

    /**
     * 分页查询 Banner。
     *
     * <p>流程：规范化关键词 → 规范化分页 → 先 count 再取当页 → 批量补投放市信息。
     * 排序固定为 {@code sortOrder} 升序、同值再按 id 升序，与门户展示顺序一致。
     *
     * @param enabled  启用筛选，{@code null} 表示启用与停用都要
     * @param scope    投放范围筛选，{@code null} 表示不筛
     * @param keyword  标题或跳转链接关键词，空白视同未传，超过 64 字抛 400
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数，上限 100
     * @return 当页 Banner 与总数
     * @throws com.suzhong.njl.web.ApiException 关键词或分页非法时 400「参数不正确」/「分页参数不正确」
     */
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
                // 运营列表与门户共用同一排序口径：数字越小越靠前，同值按 id 升序
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

    /**
     * 查询 Banner 详情，含投放市名称、slug 与当前开通态。
     *
     * @param id Banner ID
     * @return Banner 完整字段
     * @throws com.suzhong.njl.web.ApiException Banner 不存在时 404「Banner 不存在」
     */
    public BannerAdminResponse detail(Long id) {
        return toResponse(requireBanner(id));
    }

    /**
     * 新建 Banner。
     *
     * <p>流程：字段校验与排序兜底（{@link BannerRules}）→ 指定市时校验市启用且已开通
     * → 解析启用态（新建省略默认 true）→ 落库 → 写 {@code BANNER_CREATE} 审计 → 回读返回。
     *
     * @param adminId 当前运营账号 ID，写入 {@code created_by}
     * @param request Banner 写入参数
     * @return 新建后的 Banner 详情
     * @throws com.suzhong.njl.web.ApiException 字段非法时 400；市不存在 404「城市不存在」；
     *         市停用 400「地区不可用」；市未开通 400「投放市未开通，请改选全站或其他已开通市」
     */
    @Transactional
    public BannerAdminResponse create(long adminId, BannerWriteRequest request) {
        BannerRules.Prepared prepared = BannerRules.prepare(request, false);
        // 指定市投放必须在保存前确认该市当前已开通，避免建出对客永远不可见的条目
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

    /**
     * 全量更新 Banner。
     *
     * <p>流程：字段校验 → 指定市时重新校验市开通态（关城后必须改投全站或另一座已开通市）
     * → 覆盖全部可写字段 → 落库 → 写 {@code BANNER_UPDATE} 审计 → 回读返回。
     * 更新要求显式传 {@code enabled} 和 {@code sortOrder}，缺省按 400 拒绝。
     *
     * @param adminId 当前运营账号 ID，写入 {@code updated_by}
     * @param id      Banner ID
     * @param request Banner 写入参数
     * @return 更新后的 Banner 详情
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404；字段非法 400；市不存在 404；
     *         市停用或未开通 400
     */
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

    /**
     * 行内启用或停用 Banner。
     *
     * <p>流程：读取记录 → 已是目标状态直接返回且不写审计（幂等）→ 启用时若是指定市投放，
     * 校验该市当前仍开通，否则 409 → 更新启用态 → 写 {@code BANNER_ENABLE}/{@code BANNER_DISABLE} 审计。
     * 停用不受投放市开通态限制；关城本身不改 {@code enabled}。
     *
     * @param adminId 当前运营账号 ID
     * @param id      Banner ID
     * @param enabled true 启用，false 停用
     * @return 最新 Banner 详情
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404；启用时投放市已关城 409
     */
    @Transactional
    public BannerAdminResponse setEnabled(long adminId, Long id, boolean enabled) {
        HomeBanner banner = requireBanner(id);
        // 幂等：目标状态与当前一致时直接返回，不重复写审计
        if (Boolean.TRUE.equals(banner.getEnabled()) == enabled) {
            return toResponse(banner);
        }
        // 只有「启用」需要卡关城；停用始终允许，便于运营先把失效投放下线
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

    /**
     * 物理删除 Banner。
     *
     * <p>先读旧值用于审计，再按主键删除；影响行数不为 1 视为记录已被并发删除，返回 404。
     * 删除不做逻辑标记，历史审计保留删除前的投放范围与排序。
     *
     * @param adminId 当前运营账号 ID
     * @param id      Banner ID
     * @throws com.suzhong.njl.web.ApiException Banner 不存在时 404「Banner 不存在」
     */
    @Transactional
    public void delete(long adminId, Long id) {
        HomeBanner banner = requireBanner(id);
        if (homeBannerMapper.deleteById(id) != 1) {
            throw BannerMessage.NOT_FOUND.toException();
        }
        auditRecorder.record(adminId, AuditAction.BANNER_DELETE, AuditTarget.HOME_BANNER, id, detail(banner));
    }

    /** 把校验后的字段写进实体；全站投放时强制把 {@code cityId} 置空，避免脏值。 */
    private void apply(HomeBanner banner, BannerRules.Prepared prepared, boolean enabled) {
        banner.setTitle(prepared.title());
        banner.setImageUrl(prepared.imageUrl());
        banner.setLinkUrl(prepared.linkUrl());
        banner.setScope(prepared.scope());
        banner.setCityId(prepared.scope() == BannerScope.ALL ? null : prepared.cityId());
        banner.setSortOrder(prepared.sortOrder());
        banner.setEnabled(enabled);
    }

    /**
     * 校验指定市可作为投放目标：必须存在、层级为市、节点启用、且当前已开通。
     * 不存在或不是市返回 404；停用返回 400；未开通返回 400。
     */
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

    /** 只判断投放市当前是否已开通，不做存在性报错；用于启停时的宽松判断。 */
    private boolean cityCurrentlyOpen(Long cityId) {
        if (cityId == null) {
            return false;
        }
        return CityRules.isOpen(regionMapper.selectById(cityId));
    }

    /**
     * 组装列表过滤条件。关键词同时匹配标题与跳转链接，用嵌套条件保证与其他筛选项的 AND/OR 优先级正确。
     */
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

    /** 按 ID 取 Banner，不存在统一抛 404，避免各方法重复判空。 */
    private HomeBanner requireBanner(Long id) {
        HomeBanner banner = homeBannerMapper.selectById(id);
        if (banner == null) {
            throw BannerMessage.NOT_FOUND.toException();
        }
        return banner;
    }

    /**
     * 批量取当页指定市投放的市节点，避免逐条查库；全站条目与空 cityId 不参与。
     */
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

    /** 从批量结果里取当前 Banner 的投放市；全站投放返回 null。 */
    private Region cityOf(HomeBanner banner, Map<Long, Region> cities) {
        if (banner.getScope() != BannerScope.CITY || banner.getCityId() == null) {
            return null;
        }
        return cities.get(banner.getCityId());
    }

    /** 单条场景（详情、写后回读）补查投放市。 */
    private BannerAdminResponse toResponse(HomeBanner banner) {
        Region city = null;
        if (banner.getScope() == BannerScope.CITY && banner.getCityId() != null) {
            city = regionMapper.selectById(banner.getCityId());
        }
        return toResponse(banner, city);
    }

    /**
     * 实体转运营响应。{@code cityOpen} 只对指定市投放有意义：市缺失或未开通都为 false；
     * 全站投放返回 null，让前端能区分「不适用」和「已关城」。
     */
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

    /** 审计详情只放投放范围、市、启用态与排序，不写图片和链接，避免日志冗长。 */
    private Map<String, Object> detail(HomeBanner banner) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("scope", banner.getScope() == null ? null : banner.getScope().code());
        detail.put("cityId", banner.getCityId());
        detail.put("enabled", banner.getEnabled());
        detail.put("sortOrder", banner.getSortOrder());
        return detail;
    }
}
