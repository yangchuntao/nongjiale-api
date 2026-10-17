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

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreQueryService storeQueryService;

    public StoreController(StoreQueryService storeQueryService) {
        this.storeQueryService = storeQueryService;
    }

    @GetMapping
    public ApiResponse<StoreListResponse> list(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(storeQueryService.list(regionId, tag, priceMin, priceMax, sort, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(storeQueryService.detail(id));
    }
}
