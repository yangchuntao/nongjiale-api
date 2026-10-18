package com.suzhong.njl.web.portal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.HomeBannerService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.HomeBannerItem;

@RestController
@RequestMapping("/api/home/banners")
public class HomeBannerController {

    private final HomeBannerService homeBannerService;

    public HomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    @GetMapping
    public ApiResponse<List<HomeBannerItem>> list() {
        return ApiResponse.ok(homeBannerService.list());
    }
}
