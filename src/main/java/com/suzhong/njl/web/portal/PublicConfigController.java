package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.PublicConfigService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.PublicConfigResponse;

@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final PublicConfigService publicConfigService;

    public PublicConfigController(PublicConfigService publicConfigService) {
        this.publicConfigService = publicConfigService;
    }

    @GetMapping("/public")
    public ApiResponse<PublicConfigResponse> publicConfig() {
        return ApiResponse.ok(publicConfigService.publicConfig());
    }
}
