package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.MerchantApplyService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.MerchantApplyRequest;
import com.suzhong.njl.web.portal.dto.MerchantApplyResponse;

import jakarta.validation.Valid;

/**
 * 商家入驻申请接口（{@code /api/merchant-applies}）。
 *
 * <p>所属端：门户游客/意向商家，公开接口，无需 Bearer。
 *
 * <p>只接收资料并落一条待审记录，不在本接口创建账号或门店；审核与开通在运营端完成。
 * 允许选择尚未开城的市，联系电话选填，也不做重复提交限制。
 */
@RestController
@RequestMapping("/api/merchant-applies")
public class MerchantApplyController {

    private final MerchantApplyService merchantApplyService;

    public MerchantApplyController(MerchantApplyService merchantApplyService) {
        this.merchantApplyService = merchantApplyService;
    }

    /**
     * {@code POST /api/merchant-applies} — 提交入驻申请（公开）。
     *
     * <p>主体名称必填且不超过 128 字，联系人不超过 64 字，电话不超过 20 字，门店名不超过 128 字，
     * 介绍不超过 1000 字；地区可选，填了就必须存在且启用。电话填了必须是 11 位手机号。
     *
     * @param request 入驻资料；未知字段会被拒绝
     * @return 新记录 ID 与状态（固定 {@code PENDING}）
     * @throws com.suzhong.njl.web.ApiException 字段格式不合法 400；地区不存在或不可用 400「地区不存在」；
     *                                         联系电话格式不正确 400
     */
    @PostMapping
    public ApiResponse<MerchantApplyResponse> submit(@Valid @RequestBody MerchantApplyRequest request) {
        return ApiResponse.ok(merchantApplyService.submit(request));
    }
}
