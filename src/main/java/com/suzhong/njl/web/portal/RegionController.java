package com.suzhong.njl.web.portal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.RegionQueryService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.RegionItem;

/**
 * 门户地区接口（{@code /api/regions}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>只返回已开通市、这些市下仍启用的区县，以及至少有一个开通市的省；未开通市及其区县、
 * 没有开通市的省都不出现。结果走 Redis 旁路缓存，开城/关城后由服务端失效。
 */
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionQueryService regionQueryService;

    public RegionController(RegionQueryService regionQueryService) {
        this.regionQueryService = regionQueryService;
    }

    /**
     * {@code GET /api/regions} — 门户可见地区列表（公开）。
     *
     * <p>按 {@code sortOrder}、ID 升序，已含开通过滤；调用方不需要再判开通态。
     *
     * @return 省/市/区县项列表，可能为空数组；每项含父 ID、slug、层级与排序号
     */
    @GetMapping
    public ApiResponse<List<RegionItem>> list() {
        return ApiResponse.ok(regionQueryService.enabledRegions());
    }
}
