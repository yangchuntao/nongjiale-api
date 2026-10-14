package com.nongjiale.service;

import com.nongjiale.domain.Homestay;
import com.nongjiale.domain.ListingApplication;
import com.nongjiale.domain.Merchant;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.ListingApplicationRepository;
import com.nongjiale.repository.MerchantRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ListingApplicationService {
    private final ListingApplicationRepository listings;
    private final HomestayRepository homestays;
    private final MerchantRepository merchants;

    public ListingApplicationService(ListingApplicationRepository listings, HomestayRepository homestays,
                                     MerchantRepository merchants) {
        this.listings = listings;
        this.homestays = homestays;
        this.merchants = merchants;
    }

    @Transactional
    public Map<String, Object> apply(Long merchantId, Long storeId, String action, String note) {
        if (!"PUBLISH".equals(action) && !"UNPUBLISH".equals(action)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "action 须为 PUBLISH 或 UNPUBLISH");
        }
        Homestay store = homestays.findByIdAndMerchantId(storeId, merchantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "门店不存在"));
        if ("PUBLISH".equals(action) && store.isOnSale()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, 422, "门店已上架");
        }
        if ("UNPUBLISH".equals(action) && !store.isOnSale()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, 422, "门店未上架");
        }
        if (listings.existsByHomestayIdAndActionAndStatus(storeId, action, "PENDING")) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "已有待审核的相同申请");
        }
        ListingApplication app = new ListingApplication();
        app.setHomestayId(storeId);
        app.setMerchantId(merchantId);
        app.setAction(action);
        app.setStatus("PENDING");
        app.setNote(note);
        return toDto(listings.save(app), store, null);
    }

    public Page<Map<String, Object>> listMine(Long merchantId, Long storeId, String status, int page, int pageSize) {
        Page<ListingApplication> result = listings.searchMine(
                merchantId, storeId, PageRequests.blankToNull(status),
                PageRequests.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(a -> toDto(a, homestays.findById(a.getHomestayId()).orElse(null), null));
    }

    public Map<String, Object> getMine(Long merchantId, Long id) {
        ListingApplication app = listings.findById(id)
                .filter(a -> merchantId.equals(a.getMerchantId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "申请不存在"));
        return toDto(app, homestays.findById(app.getHomestayId()).orElse(null), null);
    }

    public Page<Map<String, Object>> listAdmin(String status, String action, int page, int pageSize) {
        Page<ListingApplication> result = listings.searchAdmin(
                PageRequests.blankToNull(status), PageRequests.blankToNull(action),
                PageRequests.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(this::adminDto);
    }

    public Map<String, Object> getAdmin(Long id) {
        ListingApplication app = listings.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "申请不存在"));
        return adminDto(app);
    }

    @Transactional
    public Map<String, Object> approve(Long id) {
        ListingApplication app = requirePending(id);
        Homestay store = homestays.findById(app.getHomestayId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "门店不存在"));
        if ("PUBLISH".equals(app.getAction())) {
            store.setStatus("ON_SALE");
        } else if ("UNPUBLISH".equals(app.getAction())) {
            store.setStatus("OFF_SALE");
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "action 不合法");
        }
        app.setStatus("APPROVED");
        app.setReviewedAt(Instant.now());
        homestays.save(store);
        return adminDto(app);
    }

    @Transactional
    public Map<String, Object> reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "驳回原因必填");
        }
        ListingApplication app = requirePending(id);
        app.setStatus("REJECTED");
        app.setRejectReason(reason.trim());
        app.setReviewedAt(Instant.now());
        return adminDto(app);
    }

    private ListingApplication requirePending(Long id) {
        ListingApplication app = listings.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "申请不存在"));
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "申请已处理");
        }
        return app;
    }

    private Map<String, Object> adminDto(ListingApplication app) {
        Homestay store = homestays.findById(app.getHomestayId()).orElse(null);
        Merchant merchant = merchants.findById(app.getMerchantId()).orElse(null);
        Map<String, Object> body = toDto(app, store, merchant);
        if (store != null) {
            body.put("storeStatus", store.getStatus());
            body.put("address", store.getAddress());
            body.put("regionId", store.getRegionId());
        }
        return body;
    }

    private Map<String, Object> toDto(ListingApplication app, Homestay store, Merchant merchant) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", app.getId());
        m.put("storeId", app.getHomestayId());
        m.put("storeName", store == null ? "" : store.getName());
        m.put("merchantId", app.getMerchantId());
        if (merchant != null) {
            m.put("subjectName", merchant.getSubjectName());
        }
        m.put("action", app.getAction());
        m.put("status", app.getStatus());
        m.put("note", app.getNote() == null ? "" : app.getNote());
        m.put("rejectReason", app.getRejectReason());
        m.put("createdAt", app.getCreatedAt().toString());
        m.put("reviewedAt", app.getReviewedAt() == null ? null : app.getReviewedAt().toString());
        return m;
    }
}
