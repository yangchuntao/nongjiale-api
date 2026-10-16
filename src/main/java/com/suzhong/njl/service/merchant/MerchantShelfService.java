package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.ShelfApplication;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.mapper.ShelfApplicationMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationRequest;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationResponse;

@Service
public class MerchantShelfService {

    private final MerchantAccess merchantAccess;
    private final MerchantStoreService merchantStoreService;
    private final ShelfApplicationMapper shelfApplicationMapper;
    private final StoreMapper storeMapper;

    public MerchantShelfService(
            MerchantAccess merchantAccess,
            MerchantStoreService merchantStoreService,
            ShelfApplicationMapper shelfApplicationMapper,
            StoreMapper storeMapper) {
        this.merchantAccess = merchantAccess;
        this.merchantStoreService = merchantStoreService;
        this.shelfApplicationMapper = shelfApplicationMapper;
        this.storeMapper = storeMapper;
    }

    @Transactional
    public ShelfApplicationResponse apply(AuthenticatedUser principal, Long storeId, ShelfApplicationRequest request) {
        long orgId = merchantAccess.requireOrgId(principal);
        Store store = merchantStoreService.requireOwned(orgId, storeId);
        Long pending = shelfApplicationMapper.selectCount(Wrappers.<ShelfApplication>lambdaQuery()
                .eq(ShelfApplication::getStoreId, store.getId())
                .eq(ShelfApplication::getStatus, ReviewStatus.PENDING));
        if (pending != null && pending > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "已有待审的上下架申请");
        }
        ShelfApplication application = new ShelfApplication();
        application.setStoreId(store.getId());
        application.setMerchantOrgId(orgId);
        application.setAction(request.action());
        application.setReason(request.reason());
        application.setStatus(ReviewStatus.PENDING);
        shelfApplicationMapper.insert(application);
        return toResponse(application, store.getName());
    }

    public PageResult<ShelfApplicationResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = shelfApplicationMapper.selectCount(
                Wrappers.<ShelfApplication>lambdaQuery().eq(ShelfApplication::getMerchantOrgId, orgId));
        List<ShelfApplication> rows = shelfApplicationMapper.selectList(Wrappers.<ShelfApplication>lambdaQuery()
                .eq(ShelfApplication::getMerchantOrgId, orgId)
                .orderByDesc(ShelfApplication::getCreatedAt)
                .orderByDesc(ShelfApplication::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> storeNames = storeNames(orgId, rows);
        List<ShelfApplicationResponse> items = new ArrayList<>(rows.size());
        for (ShelfApplication row : rows) {
            items.add(toResponse(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    private Map<Long, String> storeNames(long orgId, List<ShelfApplication> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> storeIds = new ArrayList<>();
        for (ShelfApplication row : rows) {
            storeIds.add(row.getStoreId());
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getMerchantOrgId, orgId)
                .in(Store::getId, storeIds));
        for (Store store : stores) {
            names.put(store.getId(), store.getName());
        }
        return names;
    }

    private ShelfApplicationResponse toResponse(ShelfApplication application, String storeName) {
        return new ShelfApplicationResponse(
                application.getId(),
                application.getStoreId(),
                storeName,
                application.getMerchantOrgId(),
                application.getAction(),
                application.getReason(),
                application.getStatus(),
                application.getRejectReason(),
                application.getCreatedAt());
    }
}
