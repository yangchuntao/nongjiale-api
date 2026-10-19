package com.suzhong.njl.service.admin;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.ShelfApplication;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.ShelfApplicationMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;
import com.suzhong.njl.web.admin.dto.ShelfApplicationAdminItem;

/**
 * 上下架申请审核服务（运营端，{@code /api/admin/shelf-applications}）。
 *
 * <p>职责：审批商家提交的上架/下架申请。通过时把门店状态改为 {@code ON_SHELF} 或 {@code OFF_SHELF}，
 * 驳回时门店可见性不变。同一门店同时只能有一条待审申请，该约束由商家提交侧保证；
 * 本类只拒绝已审核记录的重复操作。
 *
 * <p>事务边界：{@link #approve}、{@link #reject} 各一个事务，申请状态、门店状态与审计同事务；
 * 列表只读。
 *
 * <p>缓存：审核通过会改变门户可见性，提交后失效该门店详情与首页聚合；驳回不动缓存。
 * 运营直接改门店状态走 {@code AdminStoreService.update} 并写 {@code SHELF_FORCE}，不经过本类。
 */
@Service
public class AdminShelfService {

    private final ShelfApplicationMapper shelfApplicationMapper;
    private final StoreMapper storeMapper;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;
    private final Clock applicationClock;

    public AdminShelfService(
            ShelfApplicationMapper shelfApplicationMapper,
            StoreMapper storeMapper,
            AuditRecorder auditRecorder,
            AdminCache adminCache,
            Clock applicationClock) {
        this.shelfApplicationMapper = shelfApplicationMapper;
        this.storeMapper = storeMapper;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
        this.applicationClock = applicationClock;
    }

    /**
     * 分页查询上下架申请，按创建时间、id 倒序。
     *
     * @param status        申请状态，null 表示全部
     * @param storeId       门店 ID，null 表示不筛
     * @param merchantOrgId 商家主体 ID，null 表示不筛
     * @param page          页码，从 1 开始，空取 1
     * @param pageSize      每页条数，空取 20，上限 100
     * @return 当页申请（含门店名）与总数
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<ShelfApplicationAdminItem> list(
            ReviewStatus status,
            Long storeId,
            Long merchantOrgId,
            Integer page,
            Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = shelfApplicationMapper.selectCount(filter(status, storeId, merchantOrgId));
        List<ShelfApplication> rows = shelfApplicationMapper.selectList(filter(status, storeId, merchantOrgId)
                .orderByDesc(ShelfApplication::getCreatedAt)
                .orderByDesc(ShelfApplication::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> storeNames = storeNames(rows);
        List<ShelfApplicationAdminItem> items = new ArrayList<>(rows.size());
        for (ShelfApplication row : rows) {
            items.add(toItem(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 通过上下架申请。
     *
     * <p>流程：校验申请仍待审 → 取门店 → 按动作映射目标状态（{@code PUBLISH → ON_SHELF}、
     * {@code UNPUBLISH → OFF_SHELF}）→ 更新门店（带 {@code @Version} 乐观锁）
     * → 回写申请审核结果 → 写 {@code SHELF_REVIEW} 审计 → 失效门店缓存。
     *
     * @param adminId 当前运营账号 ID
     * @param id      申请 ID
     * @return 审核后的申请，状态为 {@code APPROVED}
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404「申请不存在」；门店不存在 404「门店不存在」；
     *         申请已审核 409「申请已审核」；门店并发更新冲突 409「数据已被修改」
     */
    @Transactional
    public ShelfApplicationAdminItem approve(long adminId, Long id) {
        ShelfApplication application = requirePending(id);
        Store store = storeMapper.selectById(application.getStoreId());
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        // 动作到目标状态的映射固定：上架申请落到 ON_SHELF，下架申请落到 OFF_SHELF
        ShelfStatus next = application.getAction() == ShelfAction.PUBLISH ? ShelfStatus.ON_SHELF : ShelfStatus.OFF_SHELF;
        ShelfStatus previous = store.getShelfStatus();
        store.setShelfStatus(next);
        // updateById 带 @Version，影响行数不为 1 说明门店已被商家或另一位运营改过
        int updated = storeMapper.updateById(store);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        markReviewed(application, adminId, ReviewStatus.APPROVED, null);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("outcome", ReviewStatus.APPROVED.code());
        detail.put("action", application.getAction().code());
        detail.put("storeId", store.getId());
        detail.put("from", previous == null ? "" : previous.code());
        detail.put("to", next.code());
        auditRecorder.record(adminId, AuditAction.SHELF_REVIEW, AuditTarget.SHELF_APPLICATION, application.getId(), detail);
        // 审核通过会改变门户可见性，门店详情与首页聚合都要失效
        adminCache.evictStore(store.getId());
        return toItem(application, store.getName());
    }

    /**
     * 驳回上下架申请，门店 {@code shelfStatus} 保持不变。
     *
     * <p>流程：校验申请仍待审 → 回写驳回状态、原因与审核人 → 写 {@code SHELF_REVIEW} 审计。
     *
     * @param adminId 当前运营账号 ID
     * @param id      申请 ID
     * @param request 驳回原因，必填且最长 500，由 DTO 校验
     * @return 审核后的申请，状态为 {@code REJECTED}
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；申请已审核 409
     */
    @Transactional
    public ShelfApplicationAdminItem reject(long adminId, Long id, RejectReasonRequest request) {
        ShelfApplication application = requirePending(id);
        markReviewed(application, adminId, ReviewStatus.REJECTED, request.reason().trim());
        auditRecorder.record(
                adminId,
                AuditAction.SHELF_REVIEW,
                AuditTarget.SHELF_APPLICATION,
                application.getId(),
                Map.of("outcome", ReviewStatus.REJECTED.code(), "action", application.getAction().code()));
        // 驳回只改申请，不动门店状态，也不需要失效缓存
        Store store = storeMapper.selectById(application.getStoreId());
        return toItem(application, store == null ? null : store.getName());
    }

    /** 回写申请的审核结果：状态、驳回原因、审核人与审核时间。 */
    private void markReviewed(ShelfApplication application, long adminId, ReviewStatus status, String rejectReason) {
        application.setStatus(status);
        application.setRejectReason(rejectReason);
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now(applicationClock));
        shelfApplicationMapper.updateById(application);
    }

    /** 按 ID 取申请并校验仍待审；不存在 404，已审核 409。 */
    private ShelfApplication requirePending(Long id) {
        ShelfApplication application = shelfApplicationMapper.selectById(id);
        if (application == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "申请不存在");
        }
        if (application.getStatus() != ReviewStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "申请已审核");
        }
        return application;
    }

    /** 组装申请筛选条件，空值表示不筛。 */
    private LambdaQueryWrapper<ShelfApplication> filter(ReviewStatus status, Long storeId, Long merchantOrgId) {
        LambdaQueryWrapper<ShelfApplication> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(ShelfApplication::getStatus, status);
        }
        if (storeId != null) {
            query.eq(ShelfApplication::getStoreId, storeId);
        }
        if (merchantOrgId != null) {
            query.eq(ShelfApplication::getMerchantOrgId, merchantOrgId);
        }
        return query;
    }

    /** 批量补当页门店名，避免逐条查询；门店被删时名称留空。 */
    private Map<Long, String> storeNames(List<ShelfApplication> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> ids = new ArrayList<>();
        for (ShelfApplication row : rows) {
            ids.add(row.getStoreId());
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery().in(Store::getId, ids));
        for (Store store : stores) {
            names.put(store.getId(), store.getName());
        }
        return names;
    }

    /** 实体转运营列表项，门店名由调用方批量查询后传入。 */
    private ShelfApplicationAdminItem toItem(ShelfApplication application, String storeName) {
        return new ShelfApplicationAdminItem(
                application.getId(),
                application.getStoreId(),
                storeName,
                application.getMerchantOrgId(),
                application.getAction(),
                application.getReason(),
                application.getStatus(),
                application.getRejectReason(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt());
    }
}
