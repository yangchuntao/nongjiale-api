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

/**
 * 商家入驻审核服务（运营端，{@code /api/admin/merchant-applies}）。
 *
 * <p>职责：入驻申请分页查询、通过并开通商家主体与登录账号、驳回。审核只允许发生在
 * {@code PENDING} 状态，重复审核返回 409；通过时生成或采用运营指定的明文密码，
 * 入库前经 BCrypt 哈希，明文只出现在当次响应的 {@code plainPassword} 里。
 *
 * <p>事务边界：{@link #approve}、{@link #reject} 各一个事务，主体、账号、申请状态与审计同事务，
 * 任一步失败整体回滚；列表只读。
 *
 * <p>依赖：{@link PlainPasswords} 决定本次明文（未传则生成 10 位随机密码），
 * {@link PasswordEncoder} 负责哈希；手机号唯一性由查重与数据库唯一键双重保证。
 */
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

    /**
     * 分页查询入驻申请，按创建时间、id 倒序。
     *
     * @param status   申请状态筛选，null 表示全部
     * @param page     页码，从 1 开始，空取 1
     * @param pageSize 每页条数，空取 20，上限 100
     * @return 当页申请与总数
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
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

    /**
     * 通过入驻申请并开通商家账号。
     *
     * <p>流程：校验申请仍待审 → 校验联系电话是 11 位手机号 → 查重商家账号 → 解析明文密码
     * → 新建 {@code ACTIVE} 主体 → 新建启用账号（只存 BCrypt 哈希）→ 回写申请状态与审核人
     * → 写 {@code APPLY_APPROVE} 审计。
     *
     * @param adminId 当前运营账号 ID，写入 reviewed_by 与审计
     * @param id      入驻申请 ID
     * @param request 可选明文密码；null 或空白时生成 10 位随机密码，长度由 DTO 校验
     * @return 申请 ID、主体 ID、账号 ID、登录手机号与明文密码；明文仅本次返回，不落库
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；申请已审核 409；
     *         联系电话缺失或格式不对 400「申请缺少有效联系电话」；手机号已是商家账号（含并发唯一键冲突）409
     */
    @Transactional
    public ApproveMerchantApplyResponse approve(long adminId, Long id, PlainPasswordRequest request) {
        MerchantApply apply = requirePending(id);
        String phone = apply.getContactPhone();
        // 登录号直接取申请联系电话；缺失或不是手机号就没法开号，只能先驳回补充
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "申请缺少有效联系电话");
        }
        // 先查重给出友好 409；真正的兜底是 merchant_user.phone 唯一键
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
        // 只存哈希；明文随本次响应返回一次，之后无法再查
        user.setPasswordHash(passwordEncoder.encode(plain));
        user.setDisplayName(displayName(apply));
        user.setEnabled(true);
        try {
            merchantUserMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            // 并发审核同一手机号时唯一键会冲突，转成 409，避免把数据库异常透成 500
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

    /**
     * 驳回入驻申请。
     *
     * <p>只允许待审申请；原因去掉首尾空白后落库并写 {@code APPLY_REJECT} 审计。
     * 驳回不创建主体或账号，申请人可修改后重新提交。
     *
     * @param adminId 当前运营账号 ID
     * @param id      入驻申请 ID
     * @param request 驳回原因，必填且最长 500，由 DTO 校验
     * @return 申请 ID、驳回状态与落库后的原因
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；申请已审核 409「申请已审核」
     */
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

    /** 按 ID 取申请并校验仍待审；不存在 404，已审核 409，避免重复通过或重复驳回。 */
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

    /** 组装状态筛选条件，null 表示不筛。 */
    private LambdaQueryWrapper<MerchantApply> filter(ReviewStatus status) {
        LambdaQueryWrapper<MerchantApply> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(MerchantApply::getStatus, status);
        }
        return query;
    }

    /** 账号显示名优先取联系人，缺失时回退主体名称，保证列表不出现空名。 */
    private String displayName(MerchantApply apply) {
        if (apply.getContactName() != null && !apply.getContactName().isBlank()) {
            return apply.getContactName();
        }
        return apply.getOrgName();
    }

    /** 实体转列表项。 */
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
