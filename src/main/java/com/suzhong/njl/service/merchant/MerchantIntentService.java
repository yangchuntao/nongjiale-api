package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.service.support.PhoneMask;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantIntentResponse;

@Service
public class MerchantIntentService {

    private final MerchantAccess merchantAccess;
    private final BookingIntentMapper bookingIntentMapper;
    private final StoreMapper storeMapper;

    public MerchantIntentService(
            MerchantAccess merchantAccess,
            BookingIntentMapper bookingIntentMapper,
            StoreMapper storeMapper) {
        this.merchantAccess = merchantAccess;
        this.bookingIntentMapper = bookingIntentMapper;
        this.storeMapper = storeMapper;
    }

    public PageResult<MerchantIntentResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getMerchantOrgId, orgId));
        List<BookingIntent> rows = bookingIntentMapper.selectList(Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getMerchantOrgId, orgId)
                .orderByDesc(BookingIntent::getCreatedAt)
                .orderByDesc(BookingIntent::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> storeNames = storeNames(orgId, rows);
        List<MerchantIntentResponse> items = new ArrayList<>(rows.size());
        for (BookingIntent row : rows) {
            items.add(toResponse(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public MerchantIntentResponse detail(AuthenticatedUser principal, Long id) {
        long orgId = merchantAccess.requireOrgId(principal);
        BookingIntent intent = bookingIntentMapper.selectOne(Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getId, id)
                .eq(BookingIntent::getMerchantOrgId, orgId));
        if (intent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return toResponse(intent, storeName(orgId, intent.getStoreId()));
    }

    private Map<Long, String> storeNames(long orgId, List<BookingIntent> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> storeIds = new ArrayList<>();
        for (BookingIntent row : rows) {
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

    private String storeName(long orgId, Long storeId) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery()
                .eq(Store::getId, storeId)
                .eq(Store::getMerchantOrgId, orgId));
        return store == null ? null : store.getName();
    }

    private MerchantIntentResponse toResponse(BookingIntent intent, String storeName) {
        return new MerchantIntentResponse(
                intent.getId(),
                intent.getIntentNo(),
                intent.getStatus(),
                intent.getStoreId(),
                storeName,
                intent.getCheckIn(),
                intent.getCheckOut(),
                intent.getNights(),
                intent.getGuests(),
                intent.getContactName(),
                PhoneMask.maskContact(intent.getContactPhone()),
                intent.getUserRemark(),
                intent.getCreatedAt());
    }
}
