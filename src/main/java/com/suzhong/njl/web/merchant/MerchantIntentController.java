package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantIntentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 商家端预订意向接口（{@code /api/merchant/intents}）。
 *
 * <p>所属端：商家后台。需 {@code MERCHANT} 角色 Bearer 令牌，且仅能访问本主体数据；
 * 账号或主体停用由服务层再次校验。本端只读，不能改意向状态，联系电话中间四位脱敏。
 */
@Tag(name = "商家 · 意向", description = "商家查看本主体预订意向，只读且手机号脱敏")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class MerchantIntentController {

    private final MerchantIntentService merchantIntentService;

    public MerchantIntentController(MerchantIntentService merchantIntentService) {
        this.merchantIntentService = merchantIntentService;
    }

    /**
     * {@code GET /api/merchant/intents} — 本主体意向列表。
     *
     * <p>按 {@code merchant_org_id} 隔离，创建时间倒序、ID 倒序；门店名只取本主体名下门店。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param page           页码，从 1 开始，为空取 1
     * @param pageSize       每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页意向与总数，手机号已脱敏
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用、令牌主体不符 403
     */
    @Operation(summary = "意向列表", description = "按商家主体隔离，创建时间倒序；分页上限 100，手机号中间四位脱敏。")
    @GetMapping("/api/merchant/intents")
    public ApiResponse<PageResult<MerchantIntentResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantIntentService.list(
                CurrentUser.require(authentication, UserType.MERCHANT), page, pageSize));
    }

    /**
     * {@code GET /api/merchant/intents/{id}} — 本主体意向详情。
     *
     * <p>查询同时按意向 ID 与 {@code merchant_org_id} 过滤，跨主体 ID 一律视为不存在，
     * 避免通过 ID 探测他人意向。响应中的手机号中间四位脱敏。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param id             意向 ID
     * @return 意向详情
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403；意向不存在或不属于本主体 404
     */
    @Operation(summary = "意向详情", description = "按意向 ID 与主体联合过滤，跨主体访问一律视为不存在；手机号已脱敏。")
    @GetMapping("/api/merchant/intents/{id}")
    public ApiResponse<MerchantIntentResponse> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(merchantIntentService.detail(
                CurrentUser.require(authentication, UserType.MERCHANT), id));
    }
}
