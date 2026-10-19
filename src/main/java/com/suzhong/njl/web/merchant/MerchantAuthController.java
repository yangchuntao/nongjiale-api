package com.suzhong.njl.web.merchant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.merchant.MerchantAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.merchant.dto.MerchantLoginRequest;
import com.suzhong.njl.web.merchant.dto.MerchantLoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 商家端登录接口（{@code /api/merchant/auth}）。
 *
 * <p>所属端：商家后台。登录公开；成功后签发 {@code MERCHANT} 角色 Bearer 令牌，
 * 只能访问 {@code /api/merchant/**}。本端没有注册与 {@code /auth/me}，账号由运营审核入驻后开通。
 */
@Tag(name = "商家 · 认证", description = "商家端登录，登录成功后签发 MERCHANT 角色令牌")
@RestController
@RequestMapping("/api/merchant/auth")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;

    public MerchantAuthController(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    /**
     * {@code POST /api/merchant/auth/login} — 商家手机号密码登录（公开）。
     *
     * <p>手机号须为 11 位且以 1 开头，密码必填；账号不存在与密码错误统一提示。
     * 先校验账号启用状态，再校验所属主体是否为 {@code ACTIVE}，任一停用都返回 403。
     *
     * @param request 登录请求，只接受 {@code phone} 与 {@code password}，多传未知字段会被拒绝
     * @return 令牌、过期时间、商家账号 ID、主体 ID、角色（固定 {@code MERCHANT}）与展示名
     * @throws com.suzhong.njl.web.ApiException 手机号或密码错误 401；账号或商家主体停用 403；字段格式不合法 400
     */
    @Operation(summary = "商家登录", description = "手机号密码登录；账号不存在与密码错误统一提示，账号或主体停用返回 403。")
    @PostMapping("/login")
    public ApiResponse<MerchantLoginResponse> login(@Valid @RequestBody MerchantLoginRequest request) {
        return ApiResponse.ok(merchantAuthService.login(request));
    }
}
