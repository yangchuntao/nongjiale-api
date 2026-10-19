package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.PortalAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.PortalAuthResponse;
import com.suzhong.njl.web.portal.dto.PortalLoginRequest;
import com.suzhong.njl.web.portal.dto.PortalMeResponse;
import com.suzhong.njl.web.portal.dto.PortalRegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 门户游客账号接口（{@code /api/auth}，手机号渠道）。
 *
 * <p>所属端：门户游客。注册与登录公开；{@code /api/auth/me} 需 {@code TOURIST} 角色 Bearer 令牌。
 *
 * <p>登录成功签发进程内存令牌，重启失效；角色固定 {@code TOURIST}，只能访问游客接口。
 */
@Tag(name = "门户 · 认证", description = "游客手机号注册、登录与当前身份查询")
@RestController
@RequestMapping("/api/auth")
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    public PortalAuthController(PortalAuthService portalAuthService) {
        this.portalAuthService = portalAuthService;
    }

    /**
     * {@code POST /api/auth/register} — 游客注册并直接登录（公开）。
     *
     * <p>手机号须为 11 位且以 1 开头，密码 6 到 64 位，昵称不超过 64 字；昵称为空时用脱敏手机号
     * 作为展示名。手机号全局唯一，重复注册返回 409。
     *
     * @param request 注册请求；未知字段会被拒绝
     * @return 令牌、过期时间、用户 ID、角色（固定 {@code TOURIST}）与展示名
     * @throws com.suzhong.njl.web.ApiException 字段格式不合法 400；手机号已注册 409「手机号已注册」
     */
    @Operation(summary = "游客注册", description = "手机号须 11 位且以 1 开头，密码 6 到 64 位；手机号全局唯一，重复注册返回 409。")
    @PostMapping("/register")
    public ApiResponse<PortalAuthResponse> register(@Valid @RequestBody PortalRegisterRequest request) {
        return ApiResponse.ok(portalAuthService.register(request));
    }

    /**
     * {@code POST /api/auth/login} — 游客手机号密码登录（公开）。
     *
     * <p>账号不存在、账号未设密码（微信壳账号）、密码不匹配统一返回 401，不区分文案，
     * 避免被用来探测手机号是否注册。
     *
     * @param request 登录请求，只接受 {@code phone} 与 {@code password}
     * @return 新签发的令牌与账号信息
     * @throws com.suzhong.njl.web.ApiException 字段格式不合法 400；认证失败 401「手机号或密码错误」
     */
    @Operation(summary = "游客登录", description = "账号不存在、账号未设密码或密码不匹配统一返回 401，不区分文案。")
    @PostMapping("/login")
    public ApiResponse<PortalAuthResponse> login(@Valid @RequestBody PortalLoginRequest request) {
        return ApiResponse.ok(portalAuthService.login(request));
    }

    /**
     * {@code GET /api/auth/me} — 当前游客身份。
     *
     * <p>令牌有效但账号已被删除（例如微信绑定合并时清理壳用户）时按未登录处理。
     *
     * @param authentication 当前请求的安全上下文，可为空
     * @return 用户 ID、手机号、展示名与固定角色 {@code TOURIST}
     * @throws com.suzhong.njl.web.ApiException 未登录 401；角色不是游客 403；账号已不存在 401
     */
    @Operation(summary = "当前游客身份", description = "令牌有效但账号已被删除时按未登录返回 401。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<PortalMeResponse> me(Authentication authentication) {
        return ApiResponse.ok(portalAuthService.me(CurrentUser.require(authentication, UserType.TOURIST)));
    }
}
