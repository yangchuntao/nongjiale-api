package com.nongjiale.web;

import com.nongjiale.service.MerchantAccess;
import com.nongjiale.service.MerchantAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant/auth")
public class MerchantAuthController {
    private final MerchantAccountService accounts;
    private final MerchantAccess access;

    public MerchantAuthController(MerchantAccountService accounts, MerchantAccess access) {
        this.accounts = accounts;
        this.access = access;
    }

    public record LoginReq(@NotBlank String phone, @NotBlank String password) {}
    public record PasswordReq(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        return ApiResponse.ok(accounts.login(req.phone(), req.password()));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication auth, HttpServletRequest request) {
        MerchantAccess.Context ctx = access.require(auth, request, true);
        return ApiResponse.ok(accounts.profile(ctx.user(), ctx.merchant()));
    }

    @PostMapping("/password")
    public ApiResponse<Map<String, Object>> password(@Valid @RequestBody PasswordReq req,
                                                     Authentication auth, HttpServletRequest request) {
        MerchantAccess.Context ctx = access.require(auth, request, true);
        return ApiResponse.ok(accounts.changePassword(ctx.user(), ctx.merchant(), req.oldPassword(), req.newPassword()));
    }
}
