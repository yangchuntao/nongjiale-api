package com.suzhong.njl.service.portal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.CreateIntentRequest;
import com.suzhong.njl.web.portal.dto.IntentResponse;

@Service
public class BookingIntentService {

    private final BookingIntentMapper bookingIntentMapper;
    private final PortalUserMapper portalUserMapper;
    private final StoreMapper storeMapper;
    private final StoreQueryService storeQueryService;
    private final IntentNumbers intentNumbers;
    private final Clock applicationClock;

    public BookingIntentService(
            BookingIntentMapper bookingIntentMapper,
            PortalUserMapper portalUserMapper,
            StoreMapper storeMapper,
            StoreQueryService storeQueryService,
            IntentNumbers intentNumbers,
            Clock applicationClock) {
        this.bookingIntentMapper = bookingIntentMapper;
        this.portalUserMapper = portalUserMapper;
        this.storeMapper = storeMapper;
        this.storeQueryService = storeQueryService;
        this.intentNumbers = intentNumbers;
        this.applicationClock = applicationClock;
    }

    @Transactional
    public IntentResponse create(AuthenticatedUser user, CreateIntentRequest request) {
        PortalUser owner = portalUserMapper.selectById(user.userId());
        if (owner == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (owner.getPhone() == null || owner.getPhone().isBlank()) {
            throw WechatMessage.PHONE_REQUIRED_FOR_INTENT.toException();
        }
        Store store = storeQueryService.requireBookable(request.storeId());
        if (request.checkIn().isBefore(LocalDate.now(applicationClock))) {
            throw new ApiException(HttpStatus.CONFLICT, "到店日期不正确");
        }
        if (request.checkOut() != null && request.checkOut().isBefore(request.checkIn())) {
            throw new ApiException(HttpStatus.CONFLICT, "离店日期不能早于到店日期");
        }
        if (request.checkOut() == null && request.nights() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写离店日期或晚数");
        }
        BookingIntent intent = new BookingIntent();
        intent.setUserId(user.userId());
        intent.setStoreId(store.getId());
        intent.setMerchantOrgId(store.getMerchantOrgId());
        intent.setCheckIn(request.checkIn());
        intent.setCheckOut(request.checkOut());
        intent.setNights(request.nights());
        intent.setGuests(request.guests());
        intent.setContactName(request.contactName());
        intent.setContactPhone(request.contactPhone());
        intent.setUserRemark(request.userRemark());
        intent.setStatus(IntentStatus.PENDING);
        intent.setVersion(0L);
        insertWithUniqueNo(intent);
        return toResponse(intent, store.getName());
    }

    public PageResult<IntentResponse> mine(AuthenticatedUser user, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getUserId, user.userId()));
        List<BookingIntent> rows = bookingIntentMapper.selectList(
                mineQuery(user.userId()).last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<IntentResponse> items = new ArrayList<>(rows.size());
        for (BookingIntent row : rows) {
            items.add(toResponse(row, storeName(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public IntentResponse mineOne(AuthenticatedUser user, Long id) {
        BookingIntent intent = bookingIntentMapper.selectById(id);
        if (intent == null || !user.userId().equals(intent.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return toResponse(intent, storeName(intent.getStoreId()));
    }

    private LambdaQueryWrapper<BookingIntent> mineQuery(Long userId) {
        return Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getUserId, userId)
                .orderByDesc(BookingIntent::getCreatedAt)
                .orderByDesc(BookingIntent::getId);
    }

    private void insertWithUniqueNo(BookingIntent intent) {
        for (int attempt = 0; attempt < 3; attempt++) {
            intent.setIntentNo(intentNumbers.next());
            try {
                bookingIntentMapper.insert(intent);
                return;
            } catch (DuplicateKeyException ex) {
                if (attempt == 2) {
                    throw ex;
                }
            }
        }
    }

    private String storeName(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        return store == null ? null : store.getName();
    }

    private IntentResponse toResponse(BookingIntent intent, String storeName) {
        return new IntentResponse(
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
                intent.getContactPhone(),
                intent.getUserRemark(),
                intent.getCreatedAt());
    }
}
