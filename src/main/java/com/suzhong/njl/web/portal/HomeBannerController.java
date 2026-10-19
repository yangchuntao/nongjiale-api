package com.suzhong.njl.web.portal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.HomeBannerService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.HomeBannerItem;

/**
 * 门户首页 Banner 接口（{@code /api/home/banners}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>只返回启用中的条目：全站 Banner 始终可见，指定市 Banner 仅在该市当前开通时可见；
 * 按 {@code sortOrder} 升序、ID 升序。无城市查询参数，也不进首页 feed 缓存，启停立即生效。
 */
@RestController
@RequestMapping("/api/home/banners")
public class HomeBannerController {

    private final HomeBannerService homeBannerService;

    public HomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    /**
     * {@code GET /api/home/banners} — 首页可见 Banner 列表（公开）。
     *
     * <p>投放市已关闭、地区不存在或已停用的市级条目会被隐藏；全站条目不受开城影响。
     *
     * @return Banner 列表，可能为空数组；市级条目才带城市 ID、slug 与名称
     */
    @GetMapping
    public ApiResponse<List<HomeBannerItem>> list() {
        return ApiResponse.ok(homeBannerService.list());
    }
}
