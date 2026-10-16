package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminMerchantResponse;
import com.suzhong.njl.web.admin.dto.AdminMerchantUserItem;
import com.suzhong.njl.web.admin.dto.MerchantStatusPatchRequest;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.ResetPasswordResponse;

@Service
public class AdminMerchantService {

    private final MerchantOrgMapper merchantOrgMapper;
    private final MerchantUserMapper merchantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final AuditRecorder auditRecorder;

    public AdminMerchantService(
            MerchantOrgMapper merchantOrgMapper,
            MerchantUserMapper merchantUserMapper,
            PasswordEncoder passwordEncoder,
            TokenStore tokenStore,
            AuditRecorder auditRecorder) {
        this.merchantOrgMapper = merchantOrgMapper;
        this.merchantUserMapper = merchantUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.auditRecorder = auditRecorder;
    }

    public PageResult<AdminMerchantResponse> list(MerchantOrgStatus status, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = merchantOrgMapper.selectCount(filter(status));
        List<MerchantOrg> rows = merchantOrgMapper.selectList(filter(status)
                .orderByDesc(MerchantOrg::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        return new PageResult<>(toResponses(rows), total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    @Transactional
    public AdminMerchantResponse updateStatus(long adminId, Long orgId, MerchantStatusPatchRequest request) {
        if (request == null || (request.status() == null && request.enabled() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请指定启停状态");
        }
        MerchantOrg org = requireOrg(orgId);
        if (request.status() != null) {
            org.setStatus(request.status());
            merchantOrgMapper.updateById(org);
        }
        if (request.enabled() != null) {
            List<MerchantUser> users = usersOf(orgId);
            for (MerchantUser user : users) {
                user.setEnabled(request.enabled());
                merchantUserMapper.updateById(user);
            }
        }
        boolean disabling = request.status() == MerchantOrgStatus.DISABLED || Boolean.FALSE.equals(request.enabled());
        if (disabling) {
            tokenStore.revokeMerchantOrg(orgId);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", org.getStatus().code());
        if (request.enabled() != null) {
            detail.put("enabled", request.enabled());
        }
        auditRecorder.record(
                adminId,
                disabling ? AuditAction.MERCHANT_DISABLE : AuditAction.MERCHANT_ENABLE,
                AuditTarget.MERCHANT_ORG,
                orgId,
                detail);
        return toResponse(merchantOrgMapper.selectById(orgId), usersOf(orgId));
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long orgId, PlainPasswordRequest request) {
        requireOrg(orgId);
        List<MerchantUser> users = usersOf(orgId);
        if (users.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "商家账号不存在");
        }
        String plain = PlainPasswords.resolve(request == null ? null : request.password());
        String hash = passwordEncoder.encode(plain);
        for (MerchantUser user : users) {
            user.setPasswordHash(hash);
            merchantUserMapper.updateById(user);
        }
        tokenStore.revokeMerchantOrg(orgId);
        users.sort(Comparator.comparing(MerchantUser::getId));
        MerchantUser primary = users.get(0);
        return new ResetPasswordResponse(orgId, primary.getId(), primary.getPhone(), plain);
    }

    private MerchantOrg requireOrg(Long orgId) {
        MerchantOrg org = merchantOrgMapper.selectById(orgId);
        if (org == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "商家不存在");
        }
        return org;
    }

    private LambdaQueryWrapper<MerchantOrg> filter(MerchantOrgStatus status) {
        LambdaQueryWrapper<MerchantOrg> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(MerchantOrg::getStatus, status);
        }
        return query;
    }

    private List<MerchantUser> usersOf(Long orgId) {
        return merchantUserMapper.selectList(Wrappers.<MerchantUser>lambdaQuery()
                .eq(MerchantUser::getMerchantOrgId, orgId)
                .orderByAsc(MerchantUser::getId));
    }

    private List<AdminMerchantResponse> toResponses(List<MerchantOrg> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> orgIds = new ArrayList<>(rows.size());
        for (MerchantOrg row : rows) {
            orgIds.add(row.getId());
        }
        List<MerchantUser> users = merchantUserMapper.selectList(Wrappers.<MerchantUser>lambdaQuery()
                .in(MerchantUser::getMerchantOrgId, orgIds)
                .orderByAsc(MerchantUser::getId));
        Map<Long, List<MerchantUser>> byOrg = new LinkedHashMap<>();
        for (MerchantUser user : users) {
            byOrg.computeIfAbsent(user.getMerchantOrgId(), key -> new ArrayList<>()).add(user);
        }
        List<AdminMerchantResponse> items = new ArrayList<>(rows.size());
        for (MerchantOrg row : rows) {
            items.add(toResponse(row, byOrg.getOrDefault(row.getId(), List.of())));
        }
        return items;
    }

    private AdminMerchantResponse toResponse(MerchantOrg org, List<MerchantUser> users) {
        List<AdminMerchantUserItem> items = new ArrayList<>(users.size());
        for (MerchantUser user : users) {
            items.add(new AdminMerchantUserItem(user.getId(), user.getPhone(), user.getDisplayName(), user.getEnabled()));
        }
        return new AdminMerchantResponse(
                org.getId(),
                org.getName(),
                org.getContactName(),
                org.getContactPhone(),
                org.getStatus(),
                org.getCreatedAt(),
                items);
    }
}
