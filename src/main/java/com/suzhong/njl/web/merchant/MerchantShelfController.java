package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantShelfService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationRequest;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 商家端上下架申请接口（{@code /api/merchant}）。
 *
 * <p>所属端：商家后台。需 {@code MERCHANT} 角色 Bearer 令牌，只能操作本主体名下门店。
 *
 * <p>商家不能直接改门店上下架状态，只能提交申请由运营审批；同一门店同时只允许一条待审申请，
 * 重复提交返回 409。申请期间门店可见性不变。
 */
@Tag(name = "商家 · 上下架申请", description = "商家提交上架/下架申请，由运营审批")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class MerchantShelfController {

    private final MerchantShelfService merchantShelfService;

    public MerchantShelfController(MerchantShelfService merchantShelfService) {
        this.merchantShelfService = merchantShelfService;
    }

    /**
     * {@code POST /api/merchant/stores/{id}/shelf-applications} — 提交上架/下架申请。
     *
     * <p>先校验门店属于本主体，再检查是否已有待审申请；申请原因选填，最长 500 字。
     * 动作通过后才会改写门店状态，提交本身不改变门户可见性。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param id             门店 ID，必须属于本主体
     * @param request        {@code action} 必填，取值 {@code PUBLISH}（申请上架）或 {@code UNPUBLISH}（申请下架）；
     *                       {@code reason} 可选，最长 500 字
     * @return 新建申请，含门店名、动作与 {@code PENDING} 状态
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403；
     *                                         门店不存在或不属于本主体 404；已有待审申请 409
     */
    @Operation(summary = "提交上下架申请", description = "门店须属于本主体；同一门店已有待审申请时重复提交返回 409，提交不改变门店可见性。")
    @PostMapping("/api/merchant/stores/{id}/shelf-applications")
    public ApiResponse<ShelfApplicationResponse> apply(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ShelfApplicationRequest request) {
        return ApiResponse.ok(merchantShelfService.apply(
                CurrentUser.require(authentication, UserType.MERCHANT), id, request));
    }

    /**
     * {@code GET /api/merchant/shelf-applications} — 本主体上下架申请记录。
     *
     * <p>按创建时间倒序、ID 倒序；包含已通过、已驳回与待审记录，门店名按当前门店表补全。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param page           页码，从 1 开始，为空取 1
     * @param pageSize       每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页申请与总数
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403
     */
    @Operation(summary = "申请记录", description = "含已通过、已驳回与待审记录，按创建时间倒序，门店名按当前门店表补全。")
    @GetMapping("/api/merchant/shelf-applications")
    public ApiResponse<PageResult<ShelfApplicationResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantShelfService.list(
                CurrentUser.require(authentication, UserType.MERCHANT), page, pageSize));
    }
}
