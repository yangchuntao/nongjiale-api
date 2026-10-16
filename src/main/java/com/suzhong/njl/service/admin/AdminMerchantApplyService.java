package com.suzhong.njl.service.admin;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.MerchantApply;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.mapper.MerchantApplyMapper;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ApproveMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.MerchantApplyItem;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.RejectMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;

@Service
public class AdminMerchantApplyService {

    private final MerchantApplyMapper merchantApplyMapper;
    private final MerchantOrgMapper merchantOrgMapper;
    private final MerchantUserMapper merchantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final Clock applicationClock;

    public AdminMerchantApplyService(
            MerchantApplyMapper merchantApplyMapper,
            MerchantOrgMapper merchantOrgMapper,
            MerchantUserMapper merchantUserMapper,
            PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder,
            Clock applicationClock) {
        this.merchantApplyMapper = merchantApplyMapper;
        this.merchantOrgMapper = merchantOrgMapper;
        this.merchantUserMapper = merchantUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.applicationClock = applicationClock;
    }

    public PageResult<MerchantApplyItem> list(ReviewStatus status, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = merchantApplyMapper.selectCount(filter(status));
        List<MerchantApply> rows = merchantApplyMapper.selectList(filter(status)
                .orderByDesc(MerchantApply::getCreatedAt)
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<MerchantApplyItem> items = new ArrayList<>(rows.size());
        for (MerchantApply row : rows) {
            items.add(toItem(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    @Transactional
    public ApproveMerchantApplyResponse approve(long adminId, Long id, PlainPasswordRequest request) {
        MerchantApply apply = requirePending(id);
        String phone = apply.getContactPhone();
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "申请缺少有效联系电话");
        }
        if (merchantUserMapper.selectOne(Wrappers.<MerchantUser>lambdaQuery().eq(MerchantUser::getPhone, phone)) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "该手机号已是商家账号");
        }
        String plain = PlainPasswords.resolve(request == null ? null : request.password());
        MerchantOrg org = new MerchantOrg();
        org.setName(apply.getOrgName());
        org.setContactName(apply.getContactName());
        org.setContactPhone(phone);
        org.setStatus(MerchantOrgStatus.ACTIVE);
        merchantOrgMapper.insert(org);

        MerchantUser user = new MerchantUser();
        user.setMerchantOrgId(org.getId());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(plain));
        user.setDisplayName(displayName(apply));
        user.setEnabled(true);
        try {
            merchantUserMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "该手机号已是商家账号");
        }

        apply.setStatus(ReviewStatus.APPROVED);
        apply.setReviewedBy(adminId);
        apply.setReviewedAt(LocalDateTime.now(applicationClock));
        apply.setMerchantOrgId(org.getId());
        merchantApplyMapper.updateById(apply);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("merchantOrgId", org.getId());
        detail.put("merchantUserId", user.getId());
        auditRecorder.record(adminId, AuditAction.APPLY_APPROVE, AuditTarget.MERCHANT_APPLY, apply.getId(), detail);
        return new ApproveMerchantApplyResponse(
                apply.getId(),
                ReviewStatus.APPROVED,
                org.getId(),
                user.getId(),
                phone,
                plain);
    }

    @Transactional
    public RejectMerchantApplyResponse reject(long adminId, Long id, RejectReasonRequest request) {
        MerchantApply apply = requirePending(id);
        apply.setStatus(ReviewStatus.REJECTED);
        apply.setRejectReason(request.reason().trim());
        apply.setReviewedBy(adminId);
        apply.setReviewedAt(LocalDateTime.now(applicationClock));
        merchantApplyMapper.updateById(apply);
        auditRecorder.record(
                adminId,
                AuditAction.APPLY_REJECT,
                AuditTarget.MERCHANT_APPLY,
                apply.getId(),
                Map.of("rejectReason", apply.getRejectReason()));
        return new RejectMerchantApplyResponse(apply.getId(), ReviewStatus.REJECTED, apply.getRejectReason());
    }

    private MerchantApply requirePending(Long id) {
        MerchantApply apply = merchantApplyMapper.selectById(id);
        if (apply == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "申请不存在");
        }
        if (apply.getStatus() != ReviewStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "申请已审核");
        }
        return apply;
    }

    private LambdaQueryWrapper<MerchantApply> filter(ReviewStatus status) {
        LambdaQueryWrapper<MerchantApply> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(MerchantApply::getStatus, status);
        }
        return query;
    }

    private String displayName(MerchantApply apply) {
        if (apply.getContactName() != null && !apply.getContactName().isBlank()) {
            return apply.getContactName();
        }
        return apply.getOrgName();
    }

    private MerchantApplyItem toItem(MerchantApply apply) {
        return new MerchantApplyItem(
                apply.getId(),
                apply.getOrgName(),
                apply.getContactName(),
                apply.getContactPhone(),
                apply.getPlannedStoreName(),
                apply.getRegionId(),
                apply.getDescription(),
                apply.getLicenseUrls(),
                apply.getStatus(),
                apply.getRejectReason(),
                apply.getReviewedBy(),
                apply.getReviewedAt(),
                apply.getMerchantOrgId(),
                apply.getCreatedAt());
    }
}
