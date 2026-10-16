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

    @Transactional
    public ShelfApplicationAdminItem approve(long adminId, Long id) {
        ShelfApplication application = requirePending(id);
        Store store = storeMapper.selectById(application.getStoreId());
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        ShelfStatus next = application.getAction() == ShelfAction.PUBLISH ? ShelfStatus.ON_SHELF : ShelfStatus.OFF_SHELF;
        ShelfStatus previous = store.getShelfStatus();
        store.setShelfStatus(next);
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
        adminCache.evictStore(store.getId());
        return toItem(application, store.getName());
    }

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
        Store store = storeMapper.selectById(application.getStoreId());
        return toItem(application, store == null ? null : store.getName());
    }

    private void markReviewed(ShelfApplication application, long adminId, ReviewStatus status, String rejectReason) {
        application.setStatus(status);
        application.setRejectReason(rejectReason);
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now(applicationClock));
        shelfApplicationMapper.updateById(application);
    }

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
