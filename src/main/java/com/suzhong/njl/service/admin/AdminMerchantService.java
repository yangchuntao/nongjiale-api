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

/**
 * 商家主体管理服务（运营端，{@code /api/admin/merchants}）。
 *
 * <p>职责：商家主体分页查询、启停主体或账号、重置主体下全部账号的密码。
 * 停用主体或账号时同步吊销该主体的全部内存令牌，旧 Bearer 立即失效。
 *
 * <p>事务边界：{@link #updateStatus}、{@link #resetPassword} 各一个事务，
 * 状态/密码与审计同事务；列表只读。
 *
 * <p>关键约束：{@code status} 与 {@code enabled} 至少传一个，都为空 400；
 * 重置密码会把主体下所有账号改成同一密码，明文只在当次响应返回一次。
 */
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

    /**
     * 分页查询商家主体，按 id 倒序，每个主体带全部账号。
     *
     * <p>流程：先 count → 取当页主体 → 批量查这些主体的账号 → 按主体分组组装，避免逐条查询。
     *
     * @param status   主体状态筛选，null 表示全部
     * @param page     页码，从 1 开始，空取 1
     * @param pageSize 每页条数，空取 20，上限 100
     * @return 当页主体及各自账号列表
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<AdminMerchantResponse> list(MerchantOrgStatus status, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = merchantOrgMapper.selectCount(filter(status));
        List<MerchantOrg> rows = merchantOrgMapper.selectList(filter(status)
                .orderByDesc(MerchantOrg::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        return new PageResult<>(toResponses(rows), total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 启停商家主体或该主体下的全部账号。
     *
     * <p>流程：校验至少传一个字段 → 取主体 → 按需改主体状态 → 按需改全部账号启用态
     * → 停用时吊销该主体所有令牌 → 写 {@code MERCHANT_ENABLE}/{@code MERCHANT_DISABLE} 审计 → 回读。
     * 只改主体状态时不动账号；只改账号时不动主体状态。
     *
     * @param adminId 当前运营账号 ID
     * @param orgId   商家主体 ID
     * @param request 主体状态与账号启用态，二者至少一个非空
     * @return 更新后的主体与账号列表
     * @throws com.suzhong.njl.web.ApiException 两个字段都为空 400「请指定启停状态」；
     *         主体不存在 404「商家不存在」
     */
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
        // 主体停用与账号停用任一成立都按停用处理：吊销令牌并写 MERCHANT_DISABLE
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

    /**
     * 重置商家主体下全部账号的密码。
     *
     * <p>流程：校验主体存在 → 取全部账号（至少一个）→ 解析明文 → BCrypt 哈希一次
     * → 全部账号写同一哈希 → 吊销该主体所有令牌 → 返回 id 最小账号的手机号与明文。
     *
     * @param orgId   商家主体 ID
     * @param request 可选明文密码；null 或空白时生成 10 位随机密码
     * @return 主体 ID、主号账号 ID、手机号与明文密码；明文仅本次返回，不落库
     * @throws com.suzhong.njl.web.ApiException 主体不存在 404「商家不存在」；主体下没有账号 404「商家账号不存在」
     */
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
        // 旧令牌全部作废，强制用新密码重新登录
        tokenStore.revokeMerchantOrg(orgId);
        users.sort(Comparator.comparing(MerchantUser::getId));
        // 返回 id 最小的账号作为主号，与接口文档约定一致
        MerchantUser primary = users.get(0);
        return new ResetPasswordResponse(orgId, primary.getId(), primary.getPhone(), plain);
    }

    /** 按 ID 取主体，不存在统一抛 404。 */
    private MerchantOrg requireOrg(Long orgId) {
        MerchantOrg org = merchantOrgMapper.selectById(orgId);
        if (org == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "商家不存在");
        }
        return org;
    }

    /** 组装主体状态筛选条件，null 表示不筛。 */
    private LambdaQueryWrapper<MerchantOrg> filter(MerchantOrgStatus status) {
        LambdaQueryWrapper<MerchantOrg> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(MerchantOrg::getStatus, status);
        }
        return query;
    }

    /** 取主体下全部账号，按 id 升序；主号即第一条。 */
    private List<MerchantUser> usersOf(Long orgId) {
        return merchantUserMapper.selectList(Wrappers.<MerchantUser>lambdaQuery()
                .eq(MerchantUser::getMerchantOrgId, orgId)
                .orderByAsc(MerchantUser::getId));
    }

    /** 批量组装主体与账号，一次查出当页账号后在内存分组，避免 N+1 查询。 */
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

    /** 实体转响应，账号只暴露 id、手机号、显示名与启用态，不含密码哈希。 */
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
