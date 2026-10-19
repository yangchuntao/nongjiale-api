package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminSiteConfigService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.SiteConfigResponse;
import com.suzhong.njl.web.admin.dto.SiteConfigWriteRequest;

import jakarta.validation.Valid;

/**
 * 运营端站点配置接口（{@code /api/admin/site-config}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：读取与整单更新站名、主色、客服电话。不含开城门槛等其他配置；
 * 更新只写这三项，成功后失效门户公开配置缓存。
 */
@RestController
@RequestMapping("/api/admin/site-config")
public class AdminSiteConfigController {

    private final AdminSiteConfigService adminSiteConfigService;

    public AdminSiteConfigController(AdminSiteConfigService adminSiteConfigService) {
        this.adminSiteConfigService = adminSiteConfigService;
    }

    /**
     * {@code GET /api/admin/site-config} — 读取当前站点配置。
     *
     * <p>配置按固定键从 {@code site_config} 读取；缺行或值为 null 时返回空串，不返回 null。
     *
     * @return 站名、主色（{@code #RRGGBB}）与客服电话
     */
    @GetMapping
    public ApiResponse<SiteConfigResponse> get() {
        return ApiResponse.ok(adminSiteConfigService.get());
    }

    /**
     * {@code PUT /api/admin/site-config} — 整单更新站点配置。
     *
     * <p>三项均必填：站名不超过 64 字，主色须为 {@code #RRGGBB} 形式，客服电话不超过 32 字；
     * 写入前统一去首尾空白。成功后失效 {@code site-config:public} 缓存。
     *
     * @param authentication 当前运营身份，写入配置更新人
     * @param request        站名、主色与客服电话
     * @return 更新后的三项配置
     * @throws com.suzhong.njl.web.ApiException 字段为空、超长或主色格式不正确 400
     */
    @PutMapping
    public ApiResponse<SiteConfigResponse> update(
            Authentication authentication,
            @Valid @RequestBody SiteConfigWriteRequest request) {
        return ApiResponse.ok(adminSiteConfigService.update(AdminPrincipal.adminId(authentication), request));
    }
}
