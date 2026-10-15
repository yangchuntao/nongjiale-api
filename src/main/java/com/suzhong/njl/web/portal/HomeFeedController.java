package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.HomeFeedService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.HomeFeedResponse;

@RestController
@RequestMapping("/api/home")
public class HomeFeedController {

    private final HomeFeedService homeFeedService;

    public HomeFeedController(HomeFeedService homeFeedService) {
        this.homeFeedService = homeFeedService;
    }

    @GetMapping("/feed")
    public ApiResponse<HomeFeedResponse> feed() {
        return ApiResponse.ok(homeFeedService.feed());
    }
}
