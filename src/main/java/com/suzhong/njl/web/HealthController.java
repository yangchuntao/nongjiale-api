package com.suzhong.njl.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 健康检查接口。
 *
 * <p>{@code GET /health} 公开、无鉴权，供负载均衡与容器探针使用。刻意不走
 * {@code ApiResponse} 信封：探针只认扁平结构，返回
 * {@code {"status":"UP","service":"nongjiale-api"}} 即可，失败时由进程不可达或非 200 体现。
 */
@Tag(name = "公共 · 健康检查", description = "容器与负载均衡探针，不走统一信封")
@RestController
public class HealthController {

    /** 返回固定的存活标识；不查数据库与 Redis，避免依赖抖动导致探针误杀。 */
    @Operation(summary = "健康检查", description = "固定返回 status=UP 与 service 名，不查数据库与 Redis；公开接口，无需令牌。")
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "nongjiale-api");
    }
}
