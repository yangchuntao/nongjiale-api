package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.HomeFeedService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.HomeFeedResponse;

/**
 * 门户首页聚合接口（{@code /api/home}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>把首页推荐门店与最新攻略合成一个响应，减少首屏请求数；整份内容走 Redis 旁路缓存，
 * 开城/关城与运营改推荐位后由服务端失效缓存。
 */
@RestController
@RequestMapping("/api/home")
public class HomeFeedController {

    private final HomeFeedService homeFeedService;

    public HomeFeedController(HomeFeedService homeFeedService) {
        this.homeFeedService = homeFeedService;
    }

    /**
     * {@code GET /api/home/feed} — 首页推荐内容（公开）。
     *
     * <p>推荐门店要求已上架、{@code homeRecommended=true} 且所属市已开通，按首页排序号升序；
     * 攻略只取已发布且可主推的最新 6 条，关闭市攻略不占名额。
     *
     * @return 推荐门店卡片列表与攻略摘要列表，无数据时均为空数组
     */
    @GetMapping("/feed")
    public ApiResponse<HomeFeedResponse> feed() {
        return ApiResponse.ok(homeFeedService.feed());
    }
}
