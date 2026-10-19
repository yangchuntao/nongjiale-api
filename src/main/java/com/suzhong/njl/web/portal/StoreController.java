package com.suzhong.njl.web.portal;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.StoreQueryService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.StoreDetailResponse;
import com.suzhong.njl.web.portal.dto.StoreListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 门户门店接口（{@code /api/stores}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>职责：门店列表与详情。列表只返回已开通市内的上架门店；未开通目的地返回空列表并带
 * {@code destinationStatus=CLOSED}，不抛异常。详情在下架或所属市未开通时返回 410。
 */
@Tag(name = "门户 · 门店", description = "门店列表与详情查询")
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreQueryService storeQueryService;

    public StoreController(StoreQueryService storeQueryService) {
        this.storeQueryService = storeQueryService;
    }

    /**
     * {@code GET /api/stores} — 可预订门店列表（公开）。
     *
     * <p>只返回 {@code ON_SHELF} 且落在已开通市范围内的门店，过滤与排序全部下推到 SQL。
     * 未开通的市或省内没有开通市时返回空列表，带 {@code destinationStatus=CLOSED} 与空态提示。
     *
     * @param regionId 地区 ID，可为省/市/区县；为空表示全站范围
     * @param tag      标签码，空白视同未传
     * @param priceMin 起步价下限（元），可空
     * @param priceMax 起步价上限（元），可空
     * @param sort     排序参数，取值 {@code comprehensive}/{@code price_asc}/{@code price_desc}；空值取综合排序，非法值 400
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界 400
     * @param keyword  店名关键词，空白视同未传，超过 64 字返回 400「参数不正确」
     * @return 当页门店卡片、总数、分页信息、目的地状态与空态提示
     * @throws com.suzhong.njl.web.ApiException 地区不存在/不可用、排序非法、关键词超长或分页非法 400
     */
    @Operation(summary = "门店列表", description = "只返回已开通市内的上架门店；目的地未开通返回空列表并带 destinationStatus=CLOSED，不抛异常。")
    @GetMapping
    public ApiResponse<StoreListResponse> list(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(storeQueryService.list(regionId, tag, priceMin, priceMax, sort, page, pageSize, keyword));
    }

    /**
     * {@code GET /api/stores/{id}} — 门店详情（公开）。
     *
     * <p>先校验门店可订再读缓存：非上架或所属市未开通统一 410，避免关城后仍命中旧的「可订」缓存。
     * 套餐文本以 {@code package_text} 为准；结果按门店 ID 旁路缓存。
     *
     * @param id 门店 ID
     * @return 门店详情、相册、套餐列表、设施标签与测试数据标记
     * @throws com.suzhong.njl.web.ApiException 门店不存在 404「房源不存在」；已下架或所属市未开通 410「已下架或不可用」
     */
    @Operation(summary = "门店详情", description = "门店不存在返回 404；已下架或所属市未开通统一返回 410，避免命中旧的可用缓存。")
    @GetMapping("/{id}")
    public ApiResponse<StoreDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(storeQueryService.detail(id));
    }
}
