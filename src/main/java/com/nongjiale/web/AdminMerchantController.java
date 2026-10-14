package com.nongjiale.web;

import com.nongjiale.service.MerchantAccountService;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/merchants")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminMerchantController {
    private final MerchantAccountService accounts;

    public AdminMerchantController(MerchantAccountService accounts) {
        this.accounts = accounts;
    }

    public record EnabledReq(@NotNull Boolean enabled) {}

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = accounts.adminList(page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @PostMapping("/{merchantId}/enabled")
    public ApiResponse<Map<String, Object>> enabled(@PathVariable Long merchantId, @RequestBody EnabledReq req) {
        if (req == null || req.enabled() == null) {
            throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, 400, "enabled 不能为空");
        }
        return ApiResponse.ok(accounts.setEnabled(merchantId, req.enabled()));
    }

    @PostMapping("/{merchantId}/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@PathVariable Long merchantId) {
        return ApiResponse.ok(accounts.resetPassword(merchantId));
    }
}
