package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantStoreResponse;
import com.suzhong.njl.web.merchant.dto.MerchantStoreUpdateRequest;

/**
 * 商家端门店服务（商家端）。
 *
 * <p>职责：商家查询本主体门店列表与详情、更新门店展示信息。没有创建门店接口，门店由运营
 * 审核入驻后代建并绑定主体；商家也不能直接改上下架与首页推荐，需要走上下架申请。
 *
 * <p>主体隔离：每次入口先经 {@link MerchantAccess#requireOrgId} 取当前主体 ID，列表、计数、
 * 详情与更新都以 {@code merchant_org_id} 作为过滤条件；非本主体门店统一按 404 处理，
 * 不暴露其他商家门店是否存在。
 *
 * <p>可写范围：仅限名称、地区、地址、电话、封面、相册、价格文案、起步价、套餐文案、设施、
 * 特色、须知、交通等展示字段。{@code shelfStatus}、{@code homeRecommended}、{@code homeSort}、
 * {@code testData}、{@code merchantOrgId} 在写回前按库中原值钉住，防止商家自行上架或上首页。
 *
 * <p>事务与缓存：{@link #update} 在一个事务内完成校验与更新，按 {@code version} 做乐观锁；
 * 更新成功后同步失效门店详情缓存与首页 feed 缓存，避免门户继续看到旧文案。
 */
@Service
public class MerchantStoreService {

    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final MerchantAccess merchantAccess;
    private final StoreMapper storeMapper;
    private final RegionMapper regionMapper;
    private final CacheAside cacheAside;
    private final ObjectMapper objectMapper;

    public MerchantStoreService(
            MerchantAccess merchantAccess,
            StoreMapper storeMapper,
            RegionMapper regionMapper,
            CacheAside cacheAside,
            ObjectMapper objectMapper) {
        this.merchantAccess = merchantAccess;
        this.storeMapper = storeMapper;
        this.regionMapper = regionMapper;
        this.cacheAside = cacheAside;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询本主体门店，按门店 ID 升序。
     *
     * <p>流程：解析当前主体 ID → 规范化分页参数 → 按主体 count → 取当页 → 组装响应。
     * 商家端不套用门户的「上架 + 城市开通」可见性规则，本主体的下架、草稿门店同样可见。
     *
     * @param principal 当前登录商家
     * @param page      页码，从 1 开始，为空取 1
     * @param pageSize  每页条数，为空取 20，超过 100 或小于 1 返回 400
     * @return 当页门店 + 总数 + 分页信息
     * @throws ApiException 分页参数非法时 400；账号或主体停用、越权时 403
     */
    public PageResult<MerchantStoreResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // count 与明细统一带 merchant_org_id，保证总数只统计本主体门店
        Long total = storeMapper.selectCount(Wrappers.<Store>lambdaQuery().eq(Store::getMerchantOrgId, orgId));
        List<Store> rows = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getMerchantOrgId, orgId)
                .orderByAsc(Store::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<MerchantStoreResponse> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询本主体单个门店详情。
     *
     * @param principal 当前登录商家
     * @param id        门店 ID，来自路径参数
     * @return 门店详情，包含上下架、首页推荐、测试数据标记与乐观锁版本号
     * @throws ApiException 账号或主体停用、越权时 403；门店不存在或不属于本主体时 404「门店不存在」
     */
    public MerchantStoreResponse detail(AuthenticatedUser principal, Long id) {
        long orgId = merchantAccess.requireOrgId(principal);
        return toResponse(requireOwned(orgId, id));
    }

    /**
     * 更新本主体门店的展示信息。
     *
     * <p>流程：解析当前主体 ID → 校验门店归属 → 校验目标地区存在且启用 → 记录原上下架/推荐/
     * 测试数据/归属字段 → 覆盖白名单字段 → 回填受保护字段 → 按版本号更新 → 失效缓存 →
     * 重新查询并返回最新详情。
     *
     * <p>可空字段按全量更新语义处理：未传即写 null；{@code gallery} 例外，为 null 表示保持原相册，
     * 传空数组表示清空。上下架、首页推荐、推荐排序、测试数据标记与主体归属不接受商家修改，
     * 写回前按原值还原。
     *
     * @param principal 当前登录商家
     * @param id        门店 ID，来自路径参数
     * @param request   更新请求，名称与地区必填，{@code version} 必填且须为查询时的版本号
     * @return 更新后的门店详情，{@code version} 已递增
     * @throws ApiException 账号或主体停用、越权时 403；门店不存在或不属于本主体时 404「门店不存在」；
     *                      地区不存在或未启用时 400「地区不可用」；相册序列化失败时 400；
     *                      版本号不匹配或记录已被删除时 409「数据已被修改」
     */
    @Transactional
    public MerchantStoreResponse update(AuthenticatedUser principal, Long id, MerchantStoreUpdateRequest request) {
        long orgId = merchantAccess.requireOrgId(principal);
        // 先确认门店属于本主体，后续写回才安全；非本主体门店按 404 处理
        Store store = requireOwned(orgId, id);
        // 地区必须存在且启用，防止把门店挂到未开通或已停用的地区
        Region region = regionMapper.selectById(request.regionId());
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        // 先快照受保护字段，下面覆盖白名单后再原样还原
        ShelfStatusKeep shelf = ShelfStatusKeep.of(store);
        store.setName(request.name());
        store.setRegionId(request.regionId());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setCoverUrl(request.coverUrl());
        // gallery 为 null 表示不改相册；空数组是明确的「清空」语义，两者不能混为一谈
        if (request.gallery() != null) {
            store.setGalleryJson(galleryJson(request.gallery()));
        }
        store.setPriceText(request.priceText());
        store.setPriceFrom(request.priceFrom());
        store.setPackageText(request.packageText());
        store.setFacilities(request.facilities());
        store.setFeatures(request.features());
        store.setNotice(request.notice());
        store.setTrafficText(request.trafficText());
        // 乐观锁：版本号取自请求，MyBatis-Plus 会把它放进 WHERE 并自增，旧版本写入影响行数为 0
        store.setVersion(request.version());
        // 还原上下架、首页推荐、推荐排序、测试数据标记与主体归属，商家无权修改这些字段
        shelf.restore(store);
        int updated = storeMapper.updateById(store);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        // 文案参与门户展示，详情缓存与首页 feed 都要失效，避免读到旧内容
        cacheAside.evict(CacheName.STORE_DETAIL, String.valueOf(store.getId()));
        cacheAside.evict(CacheName.HOME_FEED, "default");
        // 重新查询以拿到自增后的 version，供前端下次更新回传
        return toResponse(requireOwned(orgId, id));
    }

    /**
     * 按「门店 ID + 主体 ID」取门店，非本主体或不存在统一抛 404。
     *
     * <p>供本包内上下架申请服务复用，避免各处重复写归属校验。
     */
    Store requireOwned(long orgId, Long id) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery()
                .eq(Store::getId, id)
                .eq(Store::getMerchantOrgId, orgId));
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    /** 组装商家端门店响应，相册 JSON 在此解析为 URL 列表。 */
    private MerchantStoreResponse toResponse(Store store) {
        return new MerchantStoreResponse(
                store.getId(),
                store.getMerchantOrgId(),
                store.getRegionId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getCoverUrl(),
                gallery(store.getGalleryJson()),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getPackageText(),
                store.getFacilities(),
                store.getFeatures(),
                store.getNotice(),
                store.getTrafficText(),
                store.getShelfStatus(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                store.getVersion());
    }

    /** 把相册 JSON 解析成 URL 列表，空值或解析失败降级为空列表，避免脏数据拖垮详情。 */
    private List<String> gallery(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, GALLERY_TYPE);
        } catch (Exception ex) {
            // 历史脏数据或手工改库可能写坏 JSON，这里按空相册处理，不向门户抛 500
            return List.of();
        }
    }

    /** 把相册列表序列化为 JSON 字符串；null 原样返回，序列化失败按请求非法返回 400。 */
    private String galleryJson(List<String> gallery) {
        if (gallery == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(gallery);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图集格式不正确");
        }
    }

    /**
     * 商家更新时受保护字段的快照。
     *
     * <p>商家更新只改文案和地址。上下架、首页推荐、推荐排序、测试数据标记与主体归属在写回前
     * 按原值钉住，因此即使请求体或后续代码误设这些字段，也不会由商家侧改写。
     *
     * @param shelfStatus     上下架状态原值，只能由运营或上下架审核修改
     * @param homeRecommended 是否首页推荐原值，商家侧不可改
     * @param homeSort        首页推荐排序原值，商家侧不可改
     * @param testData        测试数据标记原值，抓取/导入标记不由商家改写
     * @param merchantOrgId   归属商家主体原值，防止跨主体改绑
     */
    private record ShelfStatusKeep(com.suzhong.njl.domain.enums.ShelfStatus shelfStatus, Boolean homeRecommended, Integer homeSort, Boolean testData, Long merchantOrgId) {

        /** 从当前门店记录快照受保护字段。 */
        static ShelfStatusKeep of(Store store) {
            return new ShelfStatusKeep(
                    store.getShelfStatus(),
                    store.getHomeRecommended(),
                    store.getHomeSort(),
                    store.getTestData(),
                    store.getMerchantOrgId());
        }

        /** 把快照写回门店，覆盖商家请求可能带入的受保护字段。 */
        void restore(Store store) {
            store.setShelfStatus(shelfStatus);
            store.setHomeRecommended(homeRecommended);
            store.setHomeSort(homeSort);
            store.setTestData(testData);
            store.setMerchantOrgId(merchantOrgId);
        }
    }
}
