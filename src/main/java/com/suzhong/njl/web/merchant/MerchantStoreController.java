package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantStoreResponse;
import com.suzhong.njl.web.merchant.dto.MerchantStoreUpdateRequest;

import jakarta.validation.Valid;

/**
 * 商家端门店接口（{@code /api/merchant/stores}）。
 *
 * <p>所属端：商家后台。需 {@code MERCHANT} 角色 Bearer 令牌，所有查询与更新都按令牌中的
 * 商家主体过滤，跨主体门店一律视为不存在。
 *
 * <p>商家可维护门店资料，但不能改上下架、首页推荐、首页排序、测试数据标记与归属主体；
 * 这些字段在写回前按原值钉住。本端没有创建门店接口，建店由运营代建。
 */
@RestController
@RequestMapping("/api/merchant/stores")
public class MerchantStoreController {

    private final MerchantStoreService merchantStoreService;

    public MerchantStoreController(MerchantStoreService merchantStoreService) {
        this.merchantStoreService = merchantStoreService;
    }

    /**
     * {@code GET /api/merchant/stores} — 本主体门店列表。
     *
     * <p>包含下架与草稿门店，按门店 ID 升序；不返回其他主体的数据。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param page           页码，从 1 开始，为空取 1
     * @param pageSize       每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页门店与总数，含上下架状态、推荐位与版本号
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403
     */
    @GetMapping
    public ApiResponse<PageResult<MerchantStoreResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantStoreService.list(merchant(authentication), page, pageSize));
    }

    /**
     * {@code GET /api/merchant/stores/{id}} — 本主体门店详情。
     *
     * <p>按门店 ID 与 {@code merchant_org_id} 联合查询，跨主体访问返回 404。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param id             门店 ID
     * @return 门店完整字段
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403；门店不存在或不属于本主体 404
     */
    @GetMapping("/{id}")
    public ApiResponse<MerchantStoreResponse> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(merchantStoreService.detail(merchant(authentication), id));
    }

    /**
     * {@code PUT /api/merchant/stores/{id}} — 编辑本主体门店资料。
     *
     * <p>名称必填且不超过 128 字，地区必填且必须启用，版本号必填；地址、电话、封面、价格文案长度受限。
     * 只更新文案与地址，上下架、首页推荐/排序、测试数据与归属主体保持原值；成功后失效门店详情与首页缓存。
     *
     * @param authentication 当前商家令牌，用于解析主体 ID
     * @param id             门店 ID，必须属于本主体
     * @param request        门店资料与乐观锁版本号；不含上下架与推荐位字段
     * @return 更新后的门店详情
     * @throws com.suzhong.njl.web.ApiException 未登录 401；账号或主体停用 403；
     *                                         门店不存在或不属于本主体 404；地区不可用、字段校验失败或图集格式错误 400；
     *                                         版本冲突 409「数据已被修改」
     */
    @PutMapping("/{id}")
    public ApiResponse<MerchantStoreResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MerchantStoreUpdateRequest request) {
        return ApiResponse.ok(merchantStoreService.update(merchant(authentication), id, request));
    }

    /** 解析当前令牌的商家主体；未登录 401，非商家角色 403。 */
    private AuthenticatedUser merchant(Authentication authentication) {
        return CurrentUser.require(authentication, UserType.MERCHANT);
    }
}
