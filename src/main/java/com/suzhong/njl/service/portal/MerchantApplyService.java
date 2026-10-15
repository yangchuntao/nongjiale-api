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

@Service
public class MerchantApplyService {

    private final MerchantApplyMapper merchantApplyMapper;
    private final RegionMapper regionMapper;

    public MerchantApplyService(MerchantApplyMapper merchantApplyMapper, RegionMapper regionMapper) {
        this.merchantApplyMapper = merchantApplyMapper;
        this.regionMapper = regionMapper;
    }

    public MerchantApplyResponse submit(MerchantApplyRequest request) {
        if (request.regionId() != null) {
            Region region = regionMapper.selectById(request.regionId());
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
        apply.setStatus(ReviewStatus.PENDING);
        merchantApplyMapper.insert(apply);
        return new MerchantApplyResponse(apply.getId(), apply.getStatus());
    }
}