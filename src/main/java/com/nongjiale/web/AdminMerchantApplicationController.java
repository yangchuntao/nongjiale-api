package com.nongjiale.web;

import com.nongjiale.service.MerchantApplicationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/merchant-applications")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminMerchantApplicationController {
    private final MerchantApplicationService applications;

    public AdminMerchantApplicationController(MerchantApplicationService applications) {
        this.applications = applications;
    }

    public record RejectReq(@NotBlank(message = "驳回原因必填") String reason) {}

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = applications.adminList(status, page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(applications.adminDetail(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.ok(applications.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id, @RequestBody RejectReq req) {
        String reason = req == null ? null : req.reason();
        return ApiResponse.ok(applications.reject(id, reason));
    }
}
