package com.suzhong.njl.service.portal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.entity.StorePackage;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.StoreSort;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.mapper.StorePackageMapper;
import com.suzhong.njl.mapper.StoreSearch;
import com.suzhong.njl.service.city.CityVisibility;
import com.suzhong.njl.service.city.StoreListingScope;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.service.support.TestDataMark;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.RegionItem;
import com.suzhong.njl.web.portal.dto.StoreCard;
import com.suzhong.njl.web.portal.dto.StoreDetailResponse;
import com.suzhong.njl.web.portal.dto.StoreListResponse;
import com.suzhong.njl.web.portal.dto.StorePackageItem;

/**
 * 门店查询服务（门户端）。
 *
 * <p>职责：向 C 端游客提供门店列表与详情，把「城市是否开通、门店是否上架、是否为测试数据」
 * 这些可见性规则收敛在本类，控制器只做参数透传与信封包装。
 *
 * <p>关键约束：
 * <ul>
 *   <li>列表只返回 {@code ON_SHELF} 且落在已开通市范围内的门店；未开通的目的地返回空列表 +
 *       {@code CLOSED} + 「该目的地暂未开通」，不抛异常；</li>
 *   <li>列表不走缓存，过滤与排序全部下推到 SQL；详情走 Redis 旁路缓存，键为门店 ID；</li>
 *   <li>{@link #requireBookable(Long)} 是详情与创建意向共用的收口：非上架或城市不可订一律 410，
 *       且判断发生在读缓存之前，避免关城后仍命中旧的「可订」缓存；</li>
 *   <li>只读，无事务；缓存不可用或未启用时自动退化为直接读库（由 {@link CacheAside} 兜底）。</li>
 * </ul>
 */
@Service
public class StoreQueryService {

    private static final TypeReference<StoreDetailResponse> DETAIL_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final StorePackageMapper storePackageMapper;
    private final RegionQueryService regionQueryService;
    private final CityVisibility cityVisibility;
    private final CacheAside cacheAside;
    private final ObjectMapper objectMapper;

    public StoreQueryService(
            StoreMapper storeMapper,
            StorePackageMapper storePackageMapper,
            RegionQueryService regionQueryService,
            CityVisibility cityVisibility,
            CacheAside cacheAside,
            ObjectMapper objectMapper) {
        this.storeMapper = storeMapper;
        this.storePackageMapper = storePackageMapper;
        this.regionQueryService = regionQueryService;
        this.cityVisibility = cityVisibility;
        this.cacheAside = cacheAside;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询可预订门店。
     *
     * <p>流程：规范化关键词与排序 → 计算城市可见范围 → 范围为空直接返回空页 →
     * 先 count 再取当页 → 批量补地区名 → 组装卡片。
     *
     * @param regionId      地区 ID，可为省/市/区县；为空表示全站范围
     * @param tag           标签码，空白视同未传；过滤下推到 SQL 的 EXISTS 子查询
     * @param priceMin      起步价下限（元），可空
     * @param priceMax      起步价上限（元），可空
     * @param sort          排序参数：comprehensive / price_asc / price_desc，空值取综合排序
     * @param page          页码，从 1 开始，为空取默认值
     * @param pageSize      每页条数，为空取默认值，上限 100
     * @param keyword       店名关键词，超过 64 字抛 400；空白视同未传
     * @return 当页数据 + 总数 + 分页信息 + 目的地状态 + 空态提示
     * @throws ApiException 地区不存在或不可用 400，排序非法 400，关键词超长或分页非法 400
     */
    public StoreListResponse list(
            Long regionId,
            String tag,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sort,
            Integer page,
            Integer pageSize,
            String keyword) {
        String matchedKeyword = StoreKeywords.normalize(keyword);
        StoreSort storeSort = parseSort(sort);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        StoreListingScope scope = cityVisibility.listScope(regionId);
        if (scope.regionIds().isEmpty()) {
            // 目的地未开通（或省内没有开通市）：不查库，直接空页；CLOSED 时带上空态提示
            return new StoreListResponse(
                    List.of(), 0, pageQuery.page(), pageQuery.pageSize(), scope.destinationStatus(), scope.emptyHint());
        }
        StoreSearch query = new StoreSearch();
        // 上架状态、地区范围、标签、价格带、关键词全部下推到 SQL（见 StoreMapper.xml 的 onShelfFilters）
        query.setShelfStatus(ShelfStatus.ON_SHELF);
        query.setRegionIds(scope.regionIds());
        query.setTagCode(blankToNull(tag));
        query.setPriceMin(priceMin);
        query.setPriceMax(priceMax);
        query.setKeyword(matchedKeyword);
        // 排序只接受 StoreSort 白名单里的固定 SQL 片段，参数不直接拼进 ORDER BY
        query.applySort(storeSort);
        query.setLimit(pageQuery.pageSize());
        query.setOffset(pageQuery.offset());
        // 先 count：总数为 0 时不必再查明细，省一次 SQL
        long total = storeMapper.countOnShelf(query);
        List<Store> rows = total == 0 ? List.of() : storeMapper.searchOnShelf(query);
        Map<Long, String> regionNames = regionNames();
        List<StoreCard> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toCard(row, regionNames));
        }
        return new StoreListResponse(
                items, total, pageQuery.page(), pageQuery.pageSize(), scope.destinationStatus(), scope.emptyHint());
    }

    /**
     * 查询门店详情。
     *
     * <p>流程：先做可订校验（404 / 410）→ 再读缓存，缓存未命中时加载套餐与地区名并回填。
     * 缓存键为 {@code store-detail:{门店ID}}，TTL 见 {@code app.cache.store-detail-ttl}。
     *
     * @param id 门店 ID
     * @return 门店详情；套餐文本以 {@code package_text} 为准
     * @throws ApiException 门店不存在 404「房源不存在」；已下架或所属市未开通 410「已下架或不可用」
     */
    public StoreDetailResponse detail(Long id) {
        Store store = requireBookable(id);
        return cacheAside.get(CacheName.STORE_DETAIL, String.valueOf(id), DETAIL_TYPE, () -> loadDetail(store));
    }

    /**
     * 校验门店当前对客可订，并返回门店实体。
     *
     * <p>详情与创建意向共用这一处收口：非 {@code ON_SHELF}，或所属市未开通（区县看父市、
     * 挂省视为不可订）统一抛 410，沿用下架信封，不另造「未开通」错误体。
     *
     * @param id 门店 ID
     * @return 可订的门店
     * @throws ApiException 不存在 404；不可订 410
     */
    public Store requireBookable(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "房源不存在");
        }
        if (store.getShelfStatus() != ShelfStatus.ON_SHELF || !cityVisibility.bookableRegion(store.getRegionId())) {
            throw new ApiException(HttpStatus.GONE, "已下架或不可用");
        }
        return store;
    }

    /** 组装详情响应：套餐按排序号、ID 升序；相册解析失败降级为空列表。 */
    private StoreDetailResponse loadDetail(Store store) {
        List<StorePackage> packages = storePackageMapper.selectList(
                Wrappers.<StorePackage>lambdaQuery()
                        .eq(StorePackage::getStoreId, store.getId())
                        .orderByAsc(StorePackage::getSortOrder)
                        .orderByAsc(StorePackage::getId));
        List<StorePackageItem> items = new ArrayList<>(packages.size());
        for (StorePackage row : packages) {
            items.add(new StorePackageItem(
                    row.getId(),
                    row.getName(),
                    row.getPriceFrom(),
                    row.getDescription(),
                    row.getSortOrder()));
        }
        return new StoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getRegionId(),
                regionNames().get(store.getRegionId()),
                store.getAddress(),
                store.getPhone(),
                store.getCoverUrl(),
                gallery(store.getGalleryJson()),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getPackageText(),
                items,
                store.getFacilities(),
                store.getFeatures(),
                store.getNotice(),
                store.getTrafficText(),
                store.getShelfStatus(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                TestDataMark.label(store.getTestData()),
                store.getVersion());
    }

    /**
     * 把门店实体转成列表卡片（首页推荐也复用）。
     *
     * <p>测试标记只由 {@code test_data} 决定，与是否计入开城供给门槛无关。
     *
     * @param store       门店实体
     * @param regionNames 地区 ID 到展示名的映射
     * @return 列表卡片
     */
    StoreCard toCard(Store store, Map<Long, String> regionNames) {
        return new StoreCard(
                store.getId(),
                store.getName(),
                store.getRegionId(),
                regionNames.get(store.getRegionId()),
                store.getCoverUrl(),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                TestDataMark.label(store.getTestData()),
                store.getShelfStatus());
    }

    /** 构建地区展示名映射；只含门户可见地区，未开通市的门店拿不到名字，卡片上的 regionName 会为空。 */
    private Map<Long, String> regionNames() {
        Map<Long, String> names = new HashMap<>();
        for (RegionItem region : regionQueryService.enabledRegions()) {
            names.put(region.id(), region.name());
        }
        return names;
    }

    /** 把相册 JSON 解析成 URL 列表，空串或解析失败降级为空列表，不影响详情整体返回。 */
    private List<String> gallery(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, GALLERY_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    /** 解析排序参数；非法值统一转成 400，避免把异常类型漏给上层。 */
    private StoreSort parseSort(String sort) {
        try {
            return StoreSort.fromParam(sort);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /** 空白字符串归一成 null，便于 SQL 的判空分支跳过该条件。 */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
