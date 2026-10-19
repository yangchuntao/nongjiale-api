package com.suzhong.njl.web.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.service.admin.AdminIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminIntentDetail;
import com.suzhong.njl.web.admin.dto.AdminIntentItem;
import com.suzhong.njl.web.admin.dto.IntentNoteRequest;
import com.suzhong.njl.web.admin.dto.IntentStatusRequest;

import jakarta.validation.Valid;

/**
 * 运营端预订意向接口（{@code /api/admin/intents}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：意向列表、详情、状态机迁移、追加备注与 CSV 导出。运营可见完整手机号；
 * 状态只能按 {@code IntentStatus.canTransitTo} 的规则迁移，变更写状态备注与审计。
 */
@RestController
@RequestMapping("/api/admin/intents")
public class AdminIntentController {

    private final AdminIntentService adminIntentService;

    public AdminIntentController(AdminIntentService adminIntentService) {
        this.adminIntentService = adminIntentService;
    }

    /**
     * {@code GET /api/admin/intents} — 分页查询意向。
     *
     * <p>按更新时间倒序、ID 倒序。到店日期区间与创建日期区间均可只传一端；
     * 创建时间的结束日按次日零点开区间处理，保证当天记录全部包含。
     *
     * @param status       意向状态，取值 {@code PENDING}/{@code CONFIRMED}/{@code RESCHEDULED}/{@code CHECKED_IN}/{@code CLOSED}
     * @param storeId      门店 ID，可选
     * @param merchantOrgId 商家主体 ID，可选
     * @param checkInFrom  到店日期下限（含），可选
     * @param checkInTo    到店日期上限（含），可选
     * @param createdFrom  创建日期下限（含），可选
     * @param createdTo    创建日期上限（含当天），可选
     * @param page         页码，从 1 开始，为空取 1
     * @param pageSize     每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页意向（含完整联系人手机号、门店名）与总数
     */
    @GetMapping
    public ApiResponse<PageResult<AdminIntentItem>> list(
            @RequestParam(required = false) IntentStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) LocalDate checkInFrom,
            @RequestParam(required = false) LocalDate checkInTo,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminIntentService.list(
                status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo, page, pageSize));
    }

    /**
     * {@code GET /api/admin/intents/export} — 按当前筛选导出意向 CSV。
     *
     * <p>筛选条件与列表一致，不分页；上限 10000 条，超过返回 400「导出条数过多」。
     * 文件带 UTF-8 BOM，列含单号、门店、日期、状态、联系人与游客备注，不含佣金。
     *
     * @param status       意向状态，可选
     * @param storeId      门店 ID，可选
     * @param merchantOrgId 商家主体 ID，可选
     * @param checkInFrom  到店日期下限（含），可选
     * @param checkInTo    到店日期上限（含），可选
     * @param createdFrom  创建日期下限（含），可选
     * @param createdTo    创建日期上限（含当天），可选
     * @return {@code text/csv} 字节流，文件名 {@code intents.csv}，不走 JSON 信封
     * @throws com.suzhong.njl.web.ApiException 结果超过 10000 条 400
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) IntentStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) LocalDate checkInFrom,
            @RequestParam(required = false) LocalDate checkInTo,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo) {
        byte[] body = adminIntentService.export(
                status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo);
        ContentDisposition disposition = ContentDisposition.attachment().filename("intents.csv").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    /**
     * {@code GET /api/admin/intents/{id}} — 意向详情。
     *
     * <p>路径变量限定为数字，非数字 ID 不会进入本方法，按未知路由返回 404。
     * 详情含完整手机号、当前版本号与按 ID 升序的备注时间线。
     *
     * @param id 意向 ID
     * @return 意向详情与备注列表
     * @throws com.suzhong.njl.web.ApiException 意向不存在 404「意向不存在」
     */
    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminIntentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminIntentService.detail(id));
    }

    /**
     * {@code POST /api/admin/intents/{id}/status} — 迁移意向状态。
     *
     * <p>合法迁移：{@code PENDING} → {@code CONFIRMED}/{@code RESCHEDULED}/{@code CLOSED}；
     * {@code CONFIRMED} → {@code CHECKED_IN}/{@code CLOSED}；{@code RESCHEDULED} → {@code CONFIRMED}/{@code CLOSED}；
     * 终态不可再改。成功后写一条 {@code STATUS_CHANGE} 备注并记录审计。
     *
     * @param authentication 当前运营身份，用于备注与审计
     * @param id             意向 ID
     * @param request        目标状态必填；备注可选且不超过 1000 字；版本号必填
     * @return 迁移后的意向详情
     * @throws com.suzhong.njl.web.ApiException 意向不存在 404；目标状态或版本号缺失 400；
     *                                         迁移不合法、当前状态为空或版本冲突 409
     */
    @PostMapping("/{id:\\d+}/status")
    public ApiResponse<AdminIntentDetail> changeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody IntentStatusRequest request) {
        return ApiResponse.ok(adminIntentService.changeStatus(AdminPrincipal.adminId(authentication), id, request));
    }

    /**
     * {@code POST /api/admin/intents/{id}/notes} — 追加一条备注。
     *
     * <p>只追加，不修改历史备注，也不改变意向状态；备注动作固定 {@code COMMENT}，
     * 内容去首尾空白后落库。
     *
     * @param authentication 当前运营身份，写入备注的 {@code adminId}
     * @param id             意向 ID
     * @param request        备注内容，必填且不超过 1000 字
     * @return 追加后的意向详情（含最新备注时间线）
     * @throws com.suzhong.njl.web.ApiException 意向不存在 404；备注为空或超长 400
     */
    @PostMapping("/{id:\\d+}/notes")
    public ApiResponse<AdminIntentDetail> addNote(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody IntentNoteRequest request) {
        return ApiResponse.ok(adminIntentService.addNote(AdminPrincipal.adminId(authentication), id, request));
    }
}
