package com.nongjiale.web;

import com.nongjiale.service.ListingApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/listing-applications")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminListingApplicationController {
    private final ListingApplicationService listings;

    public AdminListingApplicationController(ListingApplicationService listings) {
        this.listings = listings;
    }

    public record RejectReq(String reason) {}

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = listings.listAdmin(status, action, page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(listings.getAdmin(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.ok(listings.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id, @RequestBody RejectReq req) {
        return ApiResponse.ok(listings.reject(id, req == null ? null : req.reason()));
    }
}
