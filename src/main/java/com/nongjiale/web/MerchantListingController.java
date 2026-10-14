package com.nongjiale.web;

import com.nongjiale.service.ListingApplicationService;
import com.nongjiale.service.MerchantAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantListingController {
    private final MerchantAccess access;
    private final ListingApplicationService listings;

    public MerchantListingController(MerchantAccess access, ListingApplicationService listings) {
        this.access = access;
        this.listings = listings;
    }

    public record ApplyReq(@NotBlank String action, String note) {}

    @PostMapping("/stores/{storeId}/listing-applications")
    public ApiResponse<Map<String, Object>> apply(@PathVariable Long storeId, @RequestBody ApplyReq req,
                                                  Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        if (req == null || req.action() == null || req.action().isBlank()) {
            throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, 400, "action 不能为空");
        }
        return ApiResponse.ok(listings.apply(merchantId, storeId, req.action(), req.note()));
    }

    @GetMapping("/listing-applications")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        Page<Map<String, Object>> result = listings.listMine(merchantId, storeId, status, page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/listing-applications/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                   Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        return ApiResponse.ok(listings.getMine(merchantId, id));
    }
}
