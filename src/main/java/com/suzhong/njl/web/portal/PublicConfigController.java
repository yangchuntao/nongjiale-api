package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.PublicConfigService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.PublicConfigResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 门户公开站点配置接口（{@code /api/config}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>供门户渲染品牌与「联系客服」；只含站名、主色、客服电话三个字段，不含开城门槛。
 * 响应走 Redis 旁路缓存，运营更新配置后由服务端失效。
 */
@Tag(name = "门户 · 站点配置", description = "门户公开站点配置查询")
@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final PublicConfigService publicConfigService;

    public PublicConfigController(PublicConfigService publicConfigService) {
        this.publicConfigService = publicConfigService;
    }

    /**
     * {@code GET /api/config/public} — 公开站点配置（公开）。
     *
     * <p>未配置的项返回空串而不是 null，前端按「未配置」降级展示；不返回开城阈值等后台配置。
     *
     * @return 站名、主色与客服电话
     */
    @Operation(summary = "站点配置", description = "只含站名、主色与客服电话，未配置项返回空串而非 null，不返回开城阈值等后台配置。")
    @GetMapping("/public")
    public ApiResponse<PublicConfigResponse> publicConfig() {
        return ApiResponse.ok(publicConfigService.publicConfig());
    }
}
