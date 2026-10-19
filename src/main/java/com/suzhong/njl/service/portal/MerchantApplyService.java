package com.suzhong.njl.service.portal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.suzhong.njl.domain.entity.MerchantApply;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.mapper.MerchantApplyMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.MerchantApplyRequest;
import com.suzhong.njl.web.portal.dto.MerchantApplyResponse;

/**
 * 商家入驻申请服务（门户端）。
 *
 * <p>职责：接收游客/商家的入驻资料，落一条待审记录供运营端处理；本类不建账号、不建门店。
 *
 * <p>关键约束：
 * <ul>
 *   <li>地区只校验「存在且 {@code enabled=true}」，不要求该市已开通；未开通市也接受入驻申请；</li>
 *   <li>联系电话选填，填了就必须是 11 位手机号；不做唯一性校验，允许重复提交；</li>
 *   <li>初始状态固定 {@code PENDING}，审核动作在运营端，事务边界为单表插入。</li>
 * </ul>
 */
@Service
public class MerchantApplyService {

    private final MerchantApplyMapper merchantApplyMapper;
    private final RegionMapper regionMapper;

    public MerchantApplyService(MerchantApplyMapper merchantApplyMapper, RegionMapper regionMapper) {
        this.merchantApplyMapper = merchantApplyMapper;
        this.regionMapper = regionMapper;
    }

    /**
     * 提交入驻申请。
     *
     * <p>流程：按需校验地区 → 校验联系电话格式 → 组装并插入待审记录。
     *
     * @param request 入驻资料：主体名、联系人、电话、拟开门店名、地区、简介
     * @return 新记录 ID 与状态（固定 PENDING）
     * @throws ApiException 地区不存在或不可用 400「地区不存在」；电话格式不对 400「联系电话格式不正确」
     */
    public MerchantApplyResponse submit(MerchantApplyRequest request) {
        if (request.regionId() != null) {
            Region region = regionMapper.selectById(request.regionId());
            // 只用 enabled 判定：入驻阶段允许选择尚未开城的市，避免商家无法提前提交
            if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "地区不存在");
            }
        }
        if (request.contactPhone() != null && !request.contactPhone().isBlank()
                && !request.contactPhone().matches("^1\\d{10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "联系电话格式不正确");
        }
        MerchantApply apply = new MerchantApply();
        apply.setOrgName(request.orgName());
        apply.setContactName(request.contactName());
        apply.setContactPhone(request.contactPhone());
        apply.setPlannedStoreName(request.plannedStoreName());
        apply.setRegionId(request.regionId());
        apply.setDescription(request.description());
        // 新申请一律待审；通过后由运营端开通账号并（可选）代建门店
        apply.setStatus(ReviewStatus.PENDING);
        merchantApplyMapper.insert(apply);
        return new MerchantApplyResponse(apply.getId(), apply.getStatus());
    }
}