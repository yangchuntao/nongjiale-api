package com.suzhong.njl.web.admin;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminCityMetricsService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.CityIntentMetricsResponse;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminCityMetricsController {

    private final AdminCityMetricsService adminCityMetricsService;

    public AdminCityMetricsController(AdminCityMetricsService adminCityMetricsService) {
        this.adminCityMetricsService = adminCityMetricsService;
    }

    @GetMapping("/city-intents")
    public ApiResponse<CityIntentMetricsResponse> summary(
            @RequestParam(required = false) String window,
            @RequestParam(required = false) Long cityId) {
        return ApiResponse.ok(adminCityMetricsService.summary(window, cityId));
    }

    @GetMapping(value = "/city-intents/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String window,
            @RequestParam(required = false) Long cityId) {
        byte[] body = adminCityMetricsService.export(window, cityId);
        ContentDisposition disposition = ContentDisposition.attachment().filename("city-intents.csv").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
