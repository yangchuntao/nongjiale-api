package com.nongjiale.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    @Value("${nongjiale.contact-phone}")
    private String contactPhone;

    @GetMapping("/public")
    public ApiResponse<Map<String, String>> publicConfig() {
        return ApiResponse.ok(Map.of("contactPhone", contactPhone));
    }
}
