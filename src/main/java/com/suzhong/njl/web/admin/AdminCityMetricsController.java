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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 运营端城市意向看板接口（{@code /api/admin/metrics}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：按时间窗汇总各市的意向数、已确认数与已入住数。统计按滚动 24 小时计算，
 * 不是自然日零点；意向按门店归属市汇总，未匹配到市的记录不计入。导出为 UTF-8 BOM 的 CSV。
 */
@Tag(name = "运营 · 指标", description = "按城市汇总预订意向并导出 CSV")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/metrics")
public class AdminCityMetricsController {

    private final AdminCityMetricsService adminCityMetricsService;

    public AdminCityMetricsController(AdminCityMetricsService adminCityMetricsService) {
        this.adminCityMetricsService = adminCityMetricsService;
    }

    /**
     * {@code GET /api/admin/metrics/city-intents} — 城市意向汇总。
     *
     * <p>返回的第一行是「合计」，其后按地区排序号、ID 升序排列各市；已确认只统计
     * {@code CONFIRMED}，已入住只统计 {@code CHECKED_IN}。
     *
     * @param window 统计窗口，取值 {@code 7}、{@code 30}、{@code ALL}；空白或未传按 {@code 7} 处理，
     *               其他值返回 400「参数不正确」
     * @param cityId 可选，限定单个市级地区；不是启用中的市返回 404「城市不存在」
     * @return 窗口代码、统计起始时间（{@code ALL} 为 null）与汇总行 + 各市行
     */
    @Operation(summary = "城市意向汇总", description = "按滚动时间窗统计，首行为合计；窗口仅支持 7、30、ALL，空白按 7 处理。")
    @GetMapping("/city-intents")
    public ApiResponse<CityIntentMetricsResponse> summary(
            @RequestParam(required = false) String window,
            @RequestParam(required = false) Long cityId) {
        return ApiResponse.ok(adminCityMetricsService.summary(window, cityId));
    }

    /**
     * {@code GET /api/admin/metrics/city-intents/export} — 导出城市意向汇总 CSV。
     *
     * <p>筛选与汇总接口完全一致，列固定为「市,意向数,已确认,已入住」，含合计行；
     * 文件带 UTF-8 BOM，Excel 直接打开不乱码。响应不走统一 JSON 信封。
     *
     * @param window 统计窗口，取值 {@code 7}、{@code 30}、{@code ALL}，空白按 {@code 7} 处理
     * @param cityId 可选，限定单个市级地区
     * @return {@code text/csv} 字节流，文件名 {@code city-intents.csv}
     * @throws com.suzhong.njl.web.ApiException 窗口非法 400；城市不存在 404
     */
    @Operation(summary = "导出城市指标", description = "筛选同汇总接口，UTF-8 BOM 编码；响应为 CSV 文件，不走 JSON 信封。")
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
