package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.AdminAuthService;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.AdminLoginRequest;
import com.suzhong.njl.web.admin.dto.AdminLoginResponse;
import com.suzhong.njl.web.admin.dto.AdminMeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

/**
 * 运营端登录与身份接口（{@code /api/admin/auth}）。
 *
 * <p>所属端：运营后台。{@code POST /login} 公开；{@code GET /me} 需 Bearer 令牌，
 * 令牌主体类型为 {@code OPERATOR}，具体角色为 {@code OPERATOR}（运营）或 {@code AGENT}（客服）。
 *
 * <p>令牌只存进程内存，默认有效期见 {@code app.security.token-ttl}，服务重启即失效。
 */
@Tag(name = "运营 · 认证", description = "运营与客服登录、查询当前身份")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * {@code POST /api/admin/auth/login} — 运营/客服手机号密码登录（公开）。
     *
     * <p>手机号须为 11 位且以 1 开头，密码必填；账号不存在与密码错误统一提示，避免枚举手机号。
     * 先校验密码再校验启用状态，已停用账号即使密码正确也返回 403。
     *
     * @param request 登录请求，只接受 {@code phone} 与 {@code password}，多传未知字段会被拒绝
     * @return 令牌、过期时间、账号 ID、后台角色与展示名；不返回密码
     * @throws com.suzhong.njl.web.ApiException 手机号或密码错误 401；账号已停用 403；字段格式不合法 400
     */
    @Operation(summary = "运营登录", description = "手机号密码登录；先校验密码再校验启用状态，停用账号返回 403，令牌服务重启即失效。")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    /**
     * {@code GET /api/admin/auth/me} — 当前运营身份。
     *
     * <p>令牌中的主体类型必须是 {@code OPERATOR}；{@code OPERATOR} 与 {@code AGENT} 两种后台角色
     * 都通过登录接口签成该类型，因此都可调用。手机号与展示名直接取令牌快照。
     *
     * @param authentication 当前请求的安全上下文，可为空
     * @return 后台账号 ID、手机号、角色与展示名
     * @throws com.suzhong.njl.web.ApiException 未携带有效令牌 401「未登录」；主体类型不符 403「无权限」
     */
    @Operation(summary = "当前运营身份", description = "令牌主体类型须为 OPERATOR，OPERATOR 与 AGENT 两种后台角色均可调用。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.userType() != UserType.OPERATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        return ApiResponse.ok(new AdminMeResponse(user.userId(), user.phone(), user.adminRole(), user.displayName()));
    }
}
