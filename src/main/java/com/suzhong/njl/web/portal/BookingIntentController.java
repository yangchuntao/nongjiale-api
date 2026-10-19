package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.BookingIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.CreateIntentRequest;
import com.suzhong.njl.web.portal.dto.IntentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 门户预订意向接口（{@code /api/booking-intents} 与 {@code /api/me/intents}）。
 *
 * <p>所属端：门户游客。全部接口需 {@code TOURIST} 角色 Bearer 令牌，未登录 401、角色不符 403。
 *
 * <p>职责：提交意向与查看「我的意向」。平台无支付、无锁房；提交前账号必须已绑定手机号，
 * 且只能查看自己的意向，他人意向按 404 处理，避免探测单号。
 */
@Tag(name = "门户 · 预订意向", description = "提交预订意向与查看我的意向")
@RestController
public class BookingIntentController {

    private final BookingIntentService bookingIntentService;

    public BookingIntentController(BookingIntentService bookingIntentService) {
        this.bookingIntentService = bookingIntentService;
    }

    /**
     * {@code POST /api/booking-intents} — 提交预订意向。
     *
     * <p>请求体校验：门店、到店日期、人数、联系人、联系电话必填，晚数不小于 1，备注不超过 500 字。
     * 业务规则：账号须已绑定手机号（否则 403 且不落库）；门店须上架且所属市已开通（否则 410）；
     * 到店日期不能早于今天，离店不能早于到店，离店日期与晚数至少给一个。初始状态 {@code PENDING}。
     *
     * @param authentication 当前游客令牌，用于取用户 ID
     * @param request        门店、日期、晚数、人数、联系人与备注
     * @return 新建意向，含生成的意向单号与门店名
     * @throws com.suzhong.njl.web.ApiException 未登录 401；未绑定手机号 403；门店不存在 404；
     *                                         门店不可订 410；日期非法 409；离店日期与晚数同时缺失、字段格式错误 400
     */
    @Operation(summary = "提交预订意向", description = "账号须已绑定手机号，否则 403 且不落库；门店须上架且所属市已开通，否则 410。初始状态 PENDING。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/booking-intents")
    public ApiResponse<IntentResponse> create(Authentication authentication, @Valid @RequestBody CreateIntentRequest request) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.create(user, request));
    }

    /**
     * {@code GET /api/me/intents} — 我的意向列表。
     *
     * <p>按当前令牌的用户 ID 隔离，创建时间倒序、ID 倒序；门店已删除时门店名为空。
     *
     * @param authentication 当前游客令牌，用于取用户 ID
     * @param page           页码，从 1 开始，为空取 1
     * @param pageSize       每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页意向与总数
     * @throws com.suzhong.njl.web.ApiException 未登录 401；角色不是游客 403
     */
    @Operation(summary = "我的意向列表", description = "按当前令牌的用户 ID 隔离，创建时间倒序；门店已删除时门店名为空。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/me/intents")
    public ApiResponse<PageResult<IntentResponse>> mine(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.mine(user, page, pageSize));
    }

    /**
     * {@code GET /api/me/intents/{id}} — 我的意向详情。
     *
     * <p>不存在与不属于当前用户都返回 404，不用 403，避免通过 ID 探测他人意向是否存在。
     *
     * @param authentication 当前游客令牌，用于取用户 ID
     * @param id             意向 ID
     * @return 意向详情与门店名
     * @throws com.suzhong.njl.web.ApiException 未登录 401；角色不是游客 403；意向不存在或不属于本人 404
     */
    @Operation(summary = "我的意向详情", description = "不存在与不属于当前用户都返回 404，避免通过 ID 探测他人意向。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/me/intents/{id}")
    public ApiResponse<IntentResponse> mineOne(Authentication authentication, @PathVariable Long id) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.mineOne(user, id));
    }
}
