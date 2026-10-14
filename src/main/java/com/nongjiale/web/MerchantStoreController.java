package com.nongjiale.web;

import com.nongjiale.service.MerchantAccess;
import com.nongjiale.service.MerchantStoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商家门店。v1.1：storeId == homestay.id。body 中的 merchantId/status/salesCount/recommendWeight 不读取。
 */
@RestController
@RequestMapping("/api/merchant/stores")
public class MerchantStoreController {
    private final MerchantAccess access;
    private final MerchantStoreService stores;

    public MerchantStoreController(MerchantAccess access, MerchantStoreService stores) {
        this.access = access;
        this.stores = stores;
    }

    public record StoreReq(
            String name,
            Long regionId,
            String address,
            String phone,
            String summary,
            String coverUrl,
            List<String> gallery,
            String facilities,
            String notice,
            String traffic,
            String surroundings,
            Long version
    ) {}

    public record PackageReq(
            @NotBlank String name,
            String description,
            @NotNull BigDecimal priceFrom,
            Integer sortOrder
    ) {}

    public record PackageUpdateReq(
            String name,
            String description,
            BigDecimal priceFrom,
            Integer sortOrder
    ) {}

    public record TagsReq(@NotNull List<String> tags) {}

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        Page<Map<String, Object>> result = stores.list(merchantId, page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody StoreReq req,
                                                   Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.create(merchantId, req.name(), req.regionId(), req.address(), req.phone(),
                req.summary(), req.coverUrl(), req.gallery(), req.facilities(), req.notice(),
                req.traffic(), req.surroundings()));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long storeId,
                                                Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.get(merchantId, storeId));
    }

    @PutMapping("/{storeId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long storeId, @RequestBody StoreReq req,
                                                   Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.update(merchantId, storeId, req.version(), req.name(), req.regionId(),
                req.address(), req.phone(), req.summary(), req.coverUrl(), req.gallery(), req.facilities(),
                req.notice(), req.traffic(), req.surroundings()));
    }

    @DeleteMapping("/{storeId}")
    public ApiResponse<Void> delete(@PathVariable Long storeId,
                                    Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        stores.delete(merchantId, storeId);
        return ApiResponse.ok();
    }

    @GetMapping("/{storeId}/packages")
    public ApiResponse<List<Map<String, Object>>> packages(@PathVariable Long storeId,
                                                           Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.packages(merchantId, storeId));
    }

    @PostMapping("/{storeId}/packages")
    public ApiResponse<Map<String, Object>> createPackage(@PathVariable Long storeId,
                                                          @Valid @RequestBody PackageReq req,
                                                          Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.createPackage(merchantId, storeId, req.name(), req.description(),
                req.priceFrom(), req.sortOrder()));
    }

    @PutMapping("/{storeId}/packages/{packageId}")
    public ApiResponse<Map<String, Object>> updatePackage(@PathVariable Long storeId, @PathVariable Long packageId,
                                                          @RequestBody PackageUpdateReq req,
                                                          Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.updatePackage(merchantId, storeId, packageId, req.name(), req.description(),
                req.priceFrom(), req.sortOrder()));
    }

    @DeleteMapping("/{storeId}/packages/{packageId}")
    public ApiResponse<Void> deletePackage(@PathVariable Long storeId, @PathVariable Long packageId,
                                           Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        stores.deletePackage(merchantId, storeId, packageId);
        return ApiResponse.ok();
    }

    @GetMapping("/{storeId}/tags")
    public ApiResponse<Map<String, Object>> tags(@PathVariable Long storeId,
                                                 Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.tags(merchantId, storeId));
    }

    @PutMapping("/{storeId}/tags")
    public ApiResponse<Map<String, Object>> replaceTags(@PathVariable Long storeId,
                                                        @Valid @RequestBody TagsReq req,
                                                        Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(stores.replaceTags(merchantId, storeId, req.tags()));
    }
}
