package com.nongjiale.web;

import com.nongjiale.service.SiteConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final SiteConfigService configs;

    public ConfigController(SiteConfigService configs) {
        this.configs = configs;
    }

    @GetMapping("/public")
    public ApiResponse<Map<String, String>> publicConfig() {
        return ApiResponse.ok(configs.publicView());
    }
}
