package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.RegionOpenChecklist;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.RegionOpenChecklistMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ChecklistPatchRequest;
import com.suzhong.njl.web.admin.dto.CityChecklistView;
import com.suzhong.njl.web.admin.dto.CityDetailResponse;
import com.suzhong.njl.web.admin.dto.CitySummaryResponse;

/**
 * 开城管理服务（运营端，{@code /api/admin/cities}）。
 *
 * <p>职责：市的开关城状态维护，以及开城清单的查询与手勾。供给数、攻略数按实时计数推导，
 * 运营只能手改①目的地数据、④客服覆盖、⑤脏数据清理三项；门槛值由 {@link OpenCityThresholds}
 * 从站点配置读取，缺行或坏值回退 {@link CityRules} 的默认值。
 *
 * <p>事务边界：{@link #patch}、{@link #open}、{@link #close} 各为一个事务，
 * 业务更新与审计写入同事务；{@link #list}、{@link #detail} 只读，不开事务。
 *
 * <p>关键约束：
 * <ul>
 *   <li>只有 {@code CITY} 层级且 {@code enabled=true} 的节点可操作，不存在或不是市返回 404，
 *       停用返回 400「地区不可用」；</li>
 *   <li>清单行先 {@code SELECT ... FOR UPDATE} 加锁，再比对 version；冲突统一 409「数据已被修改」；</li>
 *   <li>开城必须五项全达标，否则 409 并附固定顺序的缺项文案；关城不删门店、攻略、意向，也不改 enabled；</li>
 *   <li>开城/关城在事务提交后才失效地区与首页缓存，回滚不会误删缓存。</li>
 * </ul>
 */
@Service
public class AdminCityService {

    private final RegionMapper regionMapper;
    private final RegionOpenChecklistMapper checklistMapper;
    private final StoreMapper storeMapper;
    private final ArticleMapper articleMapper;
    private final OpenCityThresholds thresholds;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;

    public AdminCityService(
            RegionMapper regionMapper,
            RegionOpenChecklistMapper checklistMapper,
            StoreMapper storeMapper,
            ArticleMapper articleMapper,
            OpenCityThresholds thresholds,
            AuditRecorder auditRecorder,
            AdminCache adminCache) {
        this.regionMapper = regionMapper;
        this.checklistMapper = checklistMapper;
        this.storeMapper = storeMapper;
        this.articleMapper = articleMapper;
        this.thresholds = thresholds;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
    }

    /**
     * 分页查询开城列表，支持状态 / 省 / 市名筛选。
     *
     * <p>流程：取全部启用的市（市级行数有限，先在内存按条件过滤出 total）→
     * 按页切片 → 只对当页的市做批量计数与清单加载 → 组装返回。
     * 计数只算当页，避免请求随城市数线性放大。
     *
     * @param page     页码，null 取默认 1
     * @param pageSize 每页条数，null 取默认 20
     * @param status   可选，{@code OPEN}/{@code CLOSED}；其他取值按不过滤处理
     * @param province 可选，按省名精确匹配；空白按不过滤处理
     * @param keyword  可选，匹配省名、市名或 slug 的包含关系；空白按不过滤处理
     * @return 分页结果，按 {@code sortOrder}、id 升序
     */
    public PageResult<CitySummaryResponse> list(
            Integer page, Integer pageSize, String status, String province, String keyword) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        List<Region> cities = enabledCities();
        Map<Long, Region> byId = allRegions();
        // 状态取值宽松处理：非法值按不过滤，与前端下拉只有两态的约定一致
        String statusFilter = "OPEN".equals(status) || "CLOSED".equals(status) ? status : null;
        String provinceFilter = blankToNull(province);
        String keywordFilter = blankToNull(keyword);
        List<Region> matched = new ArrayList<>();
        for (Region city : cities) {
            String provinceName = provinceName(city, byId);
            if (statusFilter != null && !statusFilter.equals(CityRules.effective(city.getOpenStatus()).code())) {
                continue;
            }
            if (provinceFilter != null && !provinceFilter.equals(provinceName)) {
                continue;
            }
            if (keywordFilter != null && !matchesKeyword(city, provinceName, keywordFilter)) {
                continue;
            }
            matched.add(city);
        }
        int from = Math.min(pageQuery.offset(), matched.size());
        int to = Math.min(from + pageQuery.pageSize(), matched.size());
        List<Region> pageCities = matched.subList(from, to);
        Map<Long, RegionOpenChecklist> checks = checklists(pageCities);
        Map<Long, Counts> counts = countByCity(pageCities);
        List<CitySummaryResponse> items = new ArrayList<>(pageCities.size());
        for (Region city : pageCities) {
            RegionOpenChecklist checklist = checks.get(city.getId());
            Counts count = counts.get(city.getId());
            items.add(new CitySummaryResponse(
                    city.getId(),
                    provinceName(city, byId),
                    city.getName(),
                    city.getSlug(),
                    CityRules.effective(city.getOpenStatus()),
                    count.storeCount(),
                    count.articleCount(),
                    checklist == null ? null : checklist.getUpdatedAt()));
        }
        return new PageResult<>(items, matched.size(), pageQuery.page(), pageQuery.pageSize());
    }

    /** 关键字匹配省名、市名或 slug（包含关系）；省名缺失时按空串处理。 */
    private boolean matchesKeyword(Region city, String provinceName, String keyword) {
        String haystack = (provinceName == null ? "" : provinceName) + city.getName() + city.getSlug();
        return haystack.contains(keyword);
    }

    /** 空白串转 null，便于统一按「未传」处理。 */
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 查询单个市的开城详情。
     *
     * @param cityId 市 ID
     * @return 开通态、生效门槛、三项手勾、两项实时推导、缺项与可开/可关标记
     * @throws com.suzhong.njl.web.ApiException 不存在或不是市 404「城市不存在」；节点停用 400「地区不可用」
     */
    public CityDetailResponse detail(Long cityId) {
        Region city = requireEnabledCity(cityId);
        return toDetail(city, checklistMapper.selectById(cityId), countOne(city), allRegions());
    }

    /**
     * 局部更新开城清单的手勾项。
     *
     * <p>流程：校验市可用 → 拒绝只读字段 → 要求至少改一项 → 加行锁并校验版本 →
     * 合并三项手勾 → 按版本写回 → 写 {@code CITY_CHECKLIST} 审计 → 回读详情。
     * 供给达标、攻略达标不落库，请求里出现即整体拒绝。
     *
     * @param adminId 当前运营账号 ID，写入清单 updated_by 与审计
     * @param cityId  市 ID
     * @param request 三个手勾项（省略或传 null 表示不改）与必填 version
     * @return 更新后的城市详情
     * @throws com.suzhong.njl.web.ApiException 请求带 {@code supplyReady}/{@code articlesReady} 或三项都没改时 400；
     *         市不存在 404；节点停用 400；version 为空 400「版本不能为空」；版本不一致或并发插入冲突 409「数据已被修改」
     */
    @Transactional
    public CityDetailResponse patch(long adminId, Long cityId, ChecklistPatchRequest request) {
        requireEnabledCity(cityId);
        // ②③由实时计数推导，请求里出现就直接拒绝，不能静默忽略，否则前端会误以为手勾生效
        if (request.supplyReadySpecified() || request.articlesReadySpecified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CityRules.AUTO_FIELD_MESSAGE);
        }
        // 三项都没传非 null 值视为空更新，避免白刷版本号
        if (!request.hasManualChange()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请指定要更新的项");
        }
        RegionOpenChecklist locked = lockChecklist(cityId);
        assertVersion(locked, request.getVersion());
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        if (request.destinationReadySpecified() && request.getDestinationReady() != null) {
            destination = request.getDestinationReady();
        }
        if (request.supportCoveredSpecified() && request.getSupportCovered() != null) {
            support = request.getSupportCovered();
        }
        if (request.dirtyDataClearedSpecified() && request.getDirtyDataCleared() != null) {
            dirty = request.getDirtyDataCleared();
        }
        RegionOpenChecklist saved = saveChecklist(cityId, request.getVersion(), locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        auditRecorder.record(adminId, AuditAction.CITY_CHECKLIST, AuditTarget.REGION, cityId, detail);
        return toDetail(requireEnabledCity(cityId), saved, countOne(requireEnabledCity(cityId)), allRegions());
    }

    /**
     * 开通城市。
     *
     * <p>流程：加行锁取清单 → 校验市可用 → 已开通则 409 → 校验版本 → 实时计数 →
     * 五项缺项校验 → 改 {@code open_status=OPEN} → 按版本写回清单 → 写 {@code CITY_OPEN} 审计
     * → 提交后失效缓存。
     *
     * @param adminId 当前运营账号 ID
     * @param cityId  市 ID
     * @param version 清单版本；没有清单行时必须为 0
     * @return 开通后的城市详情
     * @throws com.suzhong.njl.web.ApiException 市不存在 404；节点停用 400；已开通 409「该市已开通」；
     *         version 为空 400；版本不一致 409；任一项未达标 409「未达标，不能开通：…」
     */
    @Transactional
    public CityDetailResponse open(long adminId, Long cityId, long version) {
        RegionOpenChecklist locked = lockChecklist(cityId);
        Region city = requireEnabledCity(cityId);
        // 已开通直接冲突，避免重复写审计、重复失效缓存
        if (CityRules.effective(city.getOpenStatus()) == RegionOpenStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "该市已开通");
        }
        assertVersion(locked, version);
        Counts counts = countOne(city);
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        // 门槛来自 site_config 的 open_city.min_stores / min_articles，坏配置由 OpenCityThresholds 回退默认值
        List<String> missing = CityRules.missingReasons(
                destination, support, dirty, counts.storeCount(), counts.articleCount(),
                thresholds.minStores(), thresholds.minArticles());
        // 缺项顺序固定为目的地 / 供给 / 攻略 / 客服 / 测数，前端原样展示，不做二次排序
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, CityRules.openRejectedMessage(missing));
        }
        RegionOpenStatus previous = CityRules.effective(city.getOpenStatus());
        city.setOpenStatus(RegionOpenStatus.OPEN);
        regionMapper.updateById(city);
        RegionOpenChecklist saved = saveChecklist(cityId, version, locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        detail.put("from", previous.code());
        detail.put("to", RegionOpenStatus.OPEN.code());
        auditRecorder.record(adminId, AuditAction.CITY_OPEN, AuditTarget.REGION, cityId, detail);
        evictAfterCommit();
        return toDetail(city, saved, counts, allRegions());
    }

    /**
     * 关闭城市。
     *
     * <p>流程：加行锁取清单 → 校验市可用 → 未开通则 409 → 校验版本 → 改 {@code open_status=CLOSED}
     * → 按版本写回清单 → 写 {@code CITY_CLOSE} 审计 → 提交后失效缓存。
     * 关城不删意向、门店、攻略，也不改 {@code enabled}，历史数据全部保留。
     *
     * @param adminId 当前运营账号 ID
     * @param cityId  市 ID
     * @param version 清单版本；没有清单行时必须为 0
     * @return 关城后的城市详情，此时 {@code canClose=false}
     * @throws com.suzhong.njl.web.ApiException 市不存在 404；节点停用 400；未开通 409「该市未开通」；
     *         version 为空 400；版本不一致 409「数据已被修改」
     */
    @Transactional
    public CityDetailResponse close(long adminId, Long cityId, long version) {
        RegionOpenChecklist locked = lockChecklist(cityId);
        Region city = requireEnabledCity(cityId);
        // 只有 OPEN 能关；重复关城按冲突处理，不重复写审计
        if (CityRules.effective(city.getOpenStatus()) != RegionOpenStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "该市未开通");
        }
        assertVersion(locked, version);
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        RegionOpenStatus previous = CityRules.effective(city.getOpenStatus());
        city.setOpenStatus(RegionOpenStatus.CLOSED);
        regionMapper.updateById(city);
        RegionOpenChecklist saved = saveChecklist(cityId, version, locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        detail.put("from", previous.code());
        detail.put("to", RegionOpenStatus.CLOSED.code());
        auditRecorder.record(adminId, AuditAction.CITY_CLOSE, AuditTarget.REGION, cityId, detail);
        evictAfterCommit();
        return toDetail(city, saved, countOne(city), allRegions());
    }

    /** 取全部启用的市，按 {@code sortOrder}、id 升序，与运营列表顺序一致。 */
    private List<Region> enabledCities() {
        return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .eq(Region::getEnabled, true)
                .eq(Region::getLevel, RegionLevel.CITY)
                .orderByAsc(Region::getSortOrder)
                .orderByAsc(Region::getId));
    }

    /** 校验目标是存在、层级为市且仍启用的节点；不存在或不是市 404，停用 400。 */
    private Region requireEnabledCity(Long cityId) {
        Region region = cityId == null ? null : regionMapper.selectById(cityId);
        if (region == null || region.getLevel() != RegionLevel.CITY) {
            throw new ApiException(HttpStatus.NOT_FOUND, "城市不存在");
        }
        if (!Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        return region;
    }

    /**
     * 对清单行加悲观锁（{@code SELECT ... FOR UPDATE}）。
     * 没有清单行时返回 null；加锁可防止两个运营同时插入同一市的清单。
     */
    private RegionOpenChecklist lockChecklist(Long cityId) {
        return checklistMapper.selectOne(Wrappers.<RegionOpenChecklist>lambdaQuery()
                .eq(RegionOpenChecklist::getCityId, cityId)
                .last("FOR UPDATE"));
    }

    /**
     * 校验乐观锁版本。
     *
     * <p>无清单行时约定版本为 0，传其他值说明客户端看到的是旧状态；有行时必须与库内版本一致。
     */
    private void assertVersion(RegionOpenChecklist locked, Long version) {
        if (version == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "版本不能为空");
        }
        long current = locked == null ? 0L : locked.getVersion();
        if (locked == null) {
            if (version != 0L) {
                throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
            }
            return;
        }
        if (current != version) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
    }

    /**
     * 写入开城清单：无行则插入并置初始版本 1，有行则按版本更新。
     * 插入撞唯一键或更新影响行数不为 1，都视为并发冲突并返回 409。
     */
    private RegionOpenChecklist saveChecklist(
            Long cityId,
            Long version,
            RegionOpenChecklist locked,
            boolean destinationReady,
            boolean supportCovered,
            boolean dirtyDataCleared,
            long adminId) {
        if (locked == null) {
            RegionOpenChecklist created = new RegionOpenChecklist();
            created.setCityId(cityId);
            created.setDestinationReady(destinationReady);
            created.setSupportCovered(supportCovered);
            created.setDirtyDataCleared(dirtyDataCleared);
            created.setVersion(1L);
            created.setUpdatedBy(adminId);
            try {
                checklistMapper.insert(created);
            } catch (DuplicateKeyException ex) {
                // 并发下另一个运营已插入同一城市的清单行，按版本冲突让本次请求重试
                throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
            }
            return checklistMapper.selectById(cityId);
        }
        locked.setDestinationReady(destinationReady);
        locked.setSupportCovered(supportCovered);
        locked.setDirtyDataCleared(dirtyDataCleared);
        locked.setUpdatedBy(adminId);
        // 显式写入请求带来的 version，由 @Version 在 SQL 层再比一次；影响行数不为 1 即说明版本已被推进
        locked.setVersion(version);
        int updated = checklistMapper.updateById(locked);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        return checklistMapper.selectById(cityId);
    }

    /** 读取①目的地数据手勾；没有清单行时按未勾选处理。 */
    private boolean destinationReady(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getDestinationReady());
    }

    /** 读取④客服覆盖手勾；没有清单行时按未勾选处理。 */
    private boolean supportCovered(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getSupportCovered());
    }

    /** 读取⑤脏数据清理手勾；没有清单行时按未勾选处理。 */
    private boolean dirtyDataCleared(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getDirtyDataCleared());
    }

    /** 组装审计用的三项手勾快照。 */
    private Map<String, Object> checklistDetail(boolean destination, boolean support, boolean dirty) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("destinationReady", destination);
        detail.put("supportCovered", support);
        detail.put("dirtyDataCleared", dirty);
        return detail;
    }

    /**
     * 在事务提交后失效地区与首页缓存；当前没有活动事务时直接失效。
     * 这样事务回滚时不会把缓存里仍然有效的数据删掉。
     */
    private void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            adminCache.evictCityOpen();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 事务提交后才删缓存：若事务回滚，缓存保持原值，避免删掉后被并发读回滚前的旧数据
                adminCache.evictCityOpen();
            }
        });
    }

    /**
     * 组装城市详情：门槛取生效配置，②③按实时计数推导不落库，
     * 缺项顺序由 {@link CityRules#missingReasons} 固定。
     */
    private CityDetailResponse toDetail(
            Region city,
            RegionOpenChecklist checklist,
            Counts counts,
            Map<Long, Region> byId) {
        boolean destination = destinationReady(checklist);
        boolean support = supportCovered(checklist);
        boolean dirty = dirtyDataCleared(checklist);
        int minStores = thresholds.minStores();
        int minArticles = thresholds.minArticles();
        // ②供给达标、③攻略达标是推导值：只影响展示与 canOpen，不写清单表
        boolean supplyReady = counts.storeCount() >= minStores;
        boolean articlesReady = counts.articleCount() >= minArticles;
        List<String> missing = CityRules.missingReasons(
                destination, support, dirty, counts.storeCount(), counts.articleCount(), minStores, minArticles);
        boolean open = CityRules.effective(city.getOpenStatus()) == RegionOpenStatus.OPEN;
        return new CityDetailResponse(
                city.getId(),
                provinceName(city, byId),
                city.getName(),
                city.getSlug(),
                CityRules.effective(city.getOpenStatus()),
                counts.storeCount(),
                counts.articleCount(),
                checklist == null ? null : checklist.getUpdatedAt(),
                Boolean.TRUE,
                minStores,
                minArticles,
                new CityChecklistView(destination, supplyReady, articlesReady, support, dirty),
                !open && missing.isEmpty(),
                open,
                missing,
                checklist == null || checklist.getVersion() == null ? 0L : checklist.getVersion(),
                checklist == null ? null : checklist.getUpdatedBy());
    }

    /** 单市实时计数。 */
    private Counts countOne(Region city) {
        return countByCity(List.of(city)).get(city.getId());
    }

    /**
     * 批量统计各市供给与攻略数。
     *
     * <p>只算启用地区：上架且 {@code test_data=false} 的门店、已发布攻略；
     * 门店/攻略挂在市本身或父节点为该市的区县上才计入，挂省的不计入任何市。
     */
    private Map<Long, Counts> countByCity(List<Region> cities) {
        Map<Long, Counts> result = new HashMap<>();
        if (cities.isEmpty()) {
            return result;
        }
        Set<Long> cityIds = new HashSet<>();
        for (Region city : cities) {
            cityIds.add(city.getId());
            result.put(city.getId(), new Counts(0, 0));
        }
        // 只取启用地区，停用区县下的门店和攻略不计入开城门槛
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery().eq(Region::getEnabled, true));
        Map<Long, Region> byId = new HashMap<>();
        List<Long> supplyRegionIds = new ArrayList<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
            for (Long cityId : cityIds) {
                if (CityRules.countsTowardCity(region, cityId)) {
                    supplyRegionIds.add(region.getId());
                    break;
                }
            }
        }
        if (supplyRegionIds.isEmpty()) {
            return result;
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getShelfStatus, ShelfStatus.ON_SHELF)
                .eq(Store::getTestData, Boolean.FALSE)
                .in(Store::getRegionId, supplyRegionIds));
        for (Store store : stores) {
            addStore(result, byId.get(store.getRegionId()), cityIds);
        }
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .in(Article::getRegionId, supplyRegionIds));
        for (Article article : articles) {
            addArticle(result, byId.get(article.getRegionId()), cityIds);
        }
        return result;
    }

    /** 把门店计入其归属市；不属于任何目标市时忽略。 */
    private void addStore(Map<Long, Counts> result, Region region, Set<Long> cityIds) {
        for (Long cityId : cityIds) {
            if (CityRules.countsTowardCity(region, cityId)) {
                Counts current = result.get(cityId);
                result.put(cityId, new Counts(current.storeCount() + 1, current.articleCount()));
                return;
            }
        }
    }

    /** 把攻略计入其归属市；不属于任何目标市时忽略。 */
    private void addArticle(Map<Long, Counts> result, Region region, Set<Long> cityIds) {
        for (Long cityId : cityIds) {
            if (CityRules.countsTowardCity(region, cityId)) {
                Counts current = result.get(cityId);
                result.put(cityId, new Counts(current.storeCount(), current.articleCount() + 1));
                return;
            }
        }
    }

    /** 批量取清单行，避免逐个市查询。 */
    private Map<Long, RegionOpenChecklist> checklists(List<Region> cities) {
        Map<Long, RegionOpenChecklist> byCity = new HashMap<>();
        if (cities.isEmpty()) {
            return byCity;
        }
        List<Long> ids = new ArrayList<>(cities.size());
        for (Region city : cities) {
            ids.add(city.getId());
        }
        List<RegionOpenChecklist> rows = checklistMapper.selectList(
                Wrappers.<RegionOpenChecklist>lambdaQuery().in(RegionOpenChecklist::getCityId, ids));
        for (RegionOpenChecklist row : rows) {
            byCity.put(row.getCityId(), row);
        }
        return byCity;
    }

    /** 一次性加载全部地区，用于补省名与区县归属。 */
    private Map<Long, Region> allRegions() {
        Map<Long, Region> byId = new HashMap<>();
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }

    /** 父节点恰好是省时返回省名，否则 null。 */
    private String provinceName(Region city, Map<Long, Region> byId) {
        if (city.getParentId() == null) {
            return null;
        }
        Region parent = byId.get(city.getParentId());
        if (parent == null || parent.getLevel() != RegionLevel.PROVINCE) {
            return null;
        }
        return parent.getName();
    }

    /**
     * 单市实时计数快照，用于计算开城阈值是否达标。
     *
     * <p>口径：只统计启用地区；门店限 {@code ON_SHELF} 且 {@code test_data=false}；
     * 攻略限 {@code PUBLISHED}；归属市由 {@code CityRules.countsTowardCity} 判定（市本级及其下辖区县）。
     *
     * @param storeCount   该市达标的有效供给门店数
     * @param articleCount 该市达标的已发布攻略数
     */
    private record Counts(long storeCount, long articleCount) {
    }
}
