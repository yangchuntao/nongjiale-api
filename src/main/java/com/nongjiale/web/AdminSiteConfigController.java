package com.nongjiale.web;

import com.nongjiale.service.SiteConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/site-config")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminSiteConfigController {
    private final SiteConfigService configs;

    public AdminSiteConfigController(SiteConfigService configs) {
        this.configs = configs;
    }

    public record SiteConfigReq(String siteName, String primaryColor, String contactPhone) {}

    @GetMapping
    public ApiResponse<Map<String, String>> get() {
        return ApiResponse.ok(configs.publicView());
    }

    @PutMapping
    public ApiResponse<Map<String, String>> put(@RequestBody SiteConfigReq req) {
        SiteConfigReq body = req == null ? new SiteConfigReq(null, null, null) : req;
        return ApiResponse.ok(configs.update(body.siteName(), body.primaryColor(), body.contactPhone()));
    }
}
