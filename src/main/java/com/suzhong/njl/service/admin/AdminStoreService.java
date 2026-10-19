package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminStoreCreateRequest;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreUpdateRequest;

/**
 * 门店代管服务（运营端，{@code /api/admin/stores}）。
 *
 * <p>职责：全站门店分页查询与详情、给指定商家代建门店、代编门店。代编是运营代上下架的
 * 唯一入口：{@code shelfStatus} 与当前值不同即立即生效，并写 {@code SHELF_FORCE} 审计，
 * 不走商家申请队列。
 *
 * <p>事务边界：{@link #create}、{@link #update} 各一个事务，写库与审计同事务；查询只读。
 *
 * <p>关键约束：门店必须挂在启用地区；{@code version} 由客户端回传，乐观锁冲突返回 409；
 * 图集以 JSON 数组存 {@code gallery_json}，解析失败降级为空列表。
 * 写操作后失效门店详情与首页聚合缓存。
 */
@Service
public class AdminStoreService {

    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final RegionMapper regionMapper;
    private final MerchantOrgMapper merchantOrgMapper;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;
    private final ObjectMapper objectMapper;

    public AdminStoreService(
            StoreMapper storeMapper,
            RegionMapper regionMapper,
            MerchantOrgMapper merchantOrgMapper,
            AuditRecorder auditRecorder,
            AdminCache adminCache,
            ObjectMapper objectMapper) {
        this.storeMapper = storeMapper;
        this.regionMapper = regionMapper;
        this.merchantOrgMapper = merchantOrgMapper;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询全站门店，按 id 倒序。
     *
     * @param shelfStatus     上下架状态，null 表示不筛
     * @param regionId        地区 ID，null 表示不筛
     * @param merchantOrgId   商家主体 ID，null 表示不筛
     * @param homeRecommended 是否首页推荐，null 表示不筛
     * @param name            店名关键词，trim 后按包含匹配；空白视同未传
     * @param page            页码，从 1 开始，空取 1
     * @param pageSize        每页条数，空取 20，上限 100
     * @return 当页门店与总数
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<AdminStoreResponse> list(
            ShelfStatus shelfStatus,
            Long regionId,
            Long merchantOrgId,
            Boolean homeRecommended,
            String name,
            Integer page,
            Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = storeMapper.selectCount(filter(shelfStatus, regionId, merchantOrgId, homeRecommended, name));
        List<Store> rows = storeMapper.selectList(filter(shelfStatus, regionId, merchantOrgId, homeRecommended, name)
                .orderByDesc(Store::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<AdminStoreResponse> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询门店详情。
     *
     * @param id 门店 ID
     * @return 门店完整字段，图集已从 JSON 解析为数组
     * @throws com.suzhong.njl.web.ApiException 门店不存在时 404「门店不存在」
     */
    public AdminStoreResponse detail(Long id) {
        return toResponse(requireStore(id));
    }

    /**
     * 为指定商家代建门店。
     *
     * <p>流程：校验商家主体存在 → 校验地区启用 → 按请求填充字段并套默认值
     * （上下架默认 {@code DRAFT}、推荐默认 false、首页排序默认 100、图集默认空数组、版本从 0 起）
     * → 落库 → 失效门店缓存 → 回读详情。
     *
     * @param orgId   商家主体 ID，新门店必须绑定该主体
     * @param request 门店字段；{@code name}、{@code regionId} 必填，其余可空
     * @return 新建后的门店详情
     * @throws com.suzhong.njl.web.ApiException 商家不存在 404「商家不存在」；地区不存在或停用 400「地区不可用」；
     *         图集序列化失败 400「图集格式不正确」
     */
    @Transactional
    public AdminStoreResponse create(Long orgId, AdminStoreCreateRequest request) {
        MerchantOrg org = merchantOrgMapper.selectById(orgId);
        if (org == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "商家不存在");
        }
        requireRegion(request.regionId());
        Store store = new Store();
        // 代建门店必须绑定已存在的商家主体，运营不能创建无主门店
        store.setMerchantOrgId(orgId);
        store.setRegionId(request.regionId());
        store.setName(request.name());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setCoverUrl(request.coverUrl());
        store.setGalleryJson(request.gallery() == null ? "[]" : galleryJson(request.gallery()));
        store.setPriceText(request.priceText());
        store.setPriceFrom(request.priceFrom());
        store.setPackageText(request.packageText());
        store.setFacilities(request.facilities());
        store.setFeatures(request.features());
        store.setNotice(request.notice());
        store.setTrafficText(request.trafficText());
        // 未指定上下架状态时先落草稿，避免未审核内容直接出现在门户
        store.setShelfStatus(request.shelfStatus() == null ? ShelfStatus.DRAFT : request.shelfStatus());
        store.setHomeRecommended(Boolean.TRUE.equals(request.homeRecommended()));
        store.setHomeSort(request.homeSort() == null ? 100 : request.homeSort());
        store.setTestData(Boolean.TRUE.equals(request.testData()));
        // 新店版本从 0 开始，后续代编必须回传
        store.setVersion(0L);
        storeMapper.insert(store);
        adminCache.evictStore(store.getId());
        return toResponse(requireStore(store.getId()));
    }

    /**
     * 代编门店文案、地址与首页推荐，并允许直接改 shelfStatus。
     * 上下架与当前值不同时立即生效，写 {@code SHELF_FORCE}，不改申请队列。
     *
     * <p>流程：取门店 → 校验地区 → 记录旧上下架状态 → 全量覆盖可写字段 →
     * 按 version 更新（乐观锁）→ 状态有变化才补审计 → 失效缓存 → 回读。
     * 图集不传表示保留原图集；{@code testData} 不传表示保留。
     *
     * @param adminId 当前运营账号 ID，强制上下架时写入审计
     * @param id      门店 ID
     * @param request 门店可写字段与必填 version
     * @return 更新后的门店详情
     * @throws com.suzhong.njl.web.ApiException 门店不存在 404；地区不存在或停用 400「地区不可用」；
     *         version 与库内不一致 409「数据已被修改」；图集序列化失败 400「图集格式不正确」
     */
    @Transactional
    public AdminStoreResponse update(long adminId, Long id, AdminStoreUpdateRequest request) {
        Store store = requireStore(id);
        requireRegion(request.regionId());
        // 先记旧状态，用于判断本次是否属于运营强制上下架
        ShelfStatus previousShelf = store.getShelfStatus();
        store.setName(request.name());
        store.setRegionId(request.regionId());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setCoverUrl(request.coverUrl());
        // 图集不传表示保留原图集，传空数组才是清空
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
        store.setHomeRecommended(request.homeRecommended());
        store.setHomeSort(request.homeSort());
        store.setShelfStatus(request.shelfStatus());
        if (request.testData() != null) {
            store.setTestData(request.testData());
        }
        store.setVersion(request.version());
        // @Version 在 SQL 层比对，影响行数不为 1 即版本冲突，返回 409
        int updated = storeMapper.updateById(store);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        // 只有状态真的变化才写 SHELF_FORCE，纯文案编辑不产生上下架审计
        if (previousShelf != request.shelfStatus()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("from", previousShelf == null ? "" : previousShelf.code());
            detail.put("to", request.shelfStatus().code());
            auditRecorder.record(adminId, AuditAction.SHELF_FORCE, AuditTarget.STORE, id, detail);
        }
        adminCache.evictStore(id);
        return toResponse(requireStore(id));
    }

    /** 组装门店筛选条件：名称 trim 后按包含匹配，其余条件为空时忽略。 */
    private LambdaQueryWrapper<Store> filter(
            ShelfStatus shelfStatus,
            Long regionId,
            Long merchantOrgId,
            Boolean homeRecommended,
            String name) {
        LambdaQueryWrapper<Store> query = Wrappers.lambdaQuery();
        if (shelfStatus != null) {
            query.eq(Store::getShelfStatus, shelfStatus);
        }
        if (regionId != null) {
            query.eq(Store::getRegionId, regionId);
        }
        if (merchantOrgId != null) {
            query.eq(Store::getMerchantOrgId, merchantOrgId);
        }
        if (homeRecommended != null) {
            query.eq(Store::getHomeRecommended, homeRecommended);
        }
        if (name != null && !name.isBlank()) {
            query.like(Store::getName, name.trim());
        }
        return query;
    }

    /** 按 ID 取门店，不存在统一抛 404。 */
    private Store requireStore(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    /** 校验地区存在且启用；不存在或停用都返回 400「地区不可用」。 */
    private void requireRegion(Long regionId) {
        Region region = regionMapper.selectById(regionId);
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
    }

    /** 实体转运营响应，图集从 JSON 还原为数组。 */
    private AdminStoreResponse toResponse(Store store) {
        return new AdminStoreResponse(
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

    /** 把 JSON 相册解析成列表，解析失败降级为空列表。 */
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

    /** 把图集序列化为 JSON 数组，失败按 400 处理。 */
    private String galleryJson(List<String> gallery) {
        try {
            return objectMapper.writeValueAsString(gallery);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图集格式不正确");
        }
    }
}
