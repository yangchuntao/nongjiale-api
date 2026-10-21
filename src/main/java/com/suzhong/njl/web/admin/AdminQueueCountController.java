package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminQueueCountService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.QueueCountResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 运营工作台待办计数接口（{@code /api/admin/queue-counts}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：一次返回待跟进意向、待审入驻申请、待审上下架申请三条队列的 PENDING 计数，
 * 供工作台统计卡使用；替代旧的「为取 total 各拉一个分页列表」的用法。
 */
@Tag(name = "运营 · 工作台", description = "待办队列计数")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class AdminQueueCountController {

    private final AdminQueueCountService adminQueueCountService;

    public AdminQueueCountController(AdminQueueCountService adminQueueCountService) {
        this.adminQueueCountService = adminQueueCountService;
    }

    /**
     * {@code GET /api/admin/queue-counts} — 待办队列计数。
     *
     * @return 意向、入驻申请、上下架申请三条队列的 PENDING 数量
     */
    @Operation(summary = "待办队列计数", description = "返回待跟进意向、待审入驻申请、待审上下架申请三条队列的数量。")
    @GetMapping("/api/admin/queue-counts")
    public ApiResponse<QueueCountResponse> counts(Authentication authentication) {
        // 引用 authentication 以对齐其他 admin 接口的写法；权限由 SecurityConfig 的 /api/admin/** 规则保证
        AdminPrincipal.adminId(authentication);
        return ApiResponse.ok(adminQueueCountService.pendingCounts());
    }
}
