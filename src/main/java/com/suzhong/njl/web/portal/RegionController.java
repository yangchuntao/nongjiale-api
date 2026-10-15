package com.suzhong.njl.web.portal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.RegionQueryService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.RegionItem;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionQueryService regionQueryService;

    public RegionController(RegionQueryService regionQueryService) {
        this.regionQueryService = regionQueryService;
    }

    @GetMapping
    public ApiResponse<List<RegionItem>> list() {
        return ApiResponse.ok(regionQueryService.enabledRegions());
    }
}
