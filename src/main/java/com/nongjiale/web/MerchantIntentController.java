package com.nongjiale.web;

import com.nongjiale.domain.BookingIntent;
import com.nongjiale.domain.Homestay;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.IntentNoteRepository;
import com.nongjiale.service.MerchantAccess;
import com.nongjiale.service.PageRequests;
import com.nongjiale.service.PhoneMask;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商家意向只读。写状态/跟进一律 403，且不因单据是否存在而改变状态码，避免探测。
 */
@RestController
@RequestMapping("/api/merchant/intents")
public class MerchantIntentController {
    private static final String DENY = "商家不可修改意向或写跟进";

    private final MerchantAccess access;
    private final BookingIntentRepository intents;
    private final HomestayRepository homestays;
    private final IntentNoteRepository notes;

    public MerchantIntentController(MerchantAccess access, BookingIntentRepository intents,
                                    HomestayRepository homestays, IntentNoteRepository notes) {
        this.access = access;
        this.intents = intents;
        this.homestays = homestays;
        this.notes = notes;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        Page<BookingIntent> result = intents.searchForMerchant(
                merchantId, storeId, PageRequests.blankToNull(status),
                PageRequests.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(new PageResult<>(
                result.map(bi -> toDto(bi, homestays.findById(bi.getHomestayId()).orElse(null))).getContent(),
                result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                   Authentication auth, HttpServletRequest request) {
        Long merchantId = access.require(auth, request, false).merchant().getId();
        BookingIntent bi = intents.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "意向单不存在"));
        Homestay store = homestays.findById(bi.getHomestayId()).orElse(null);
        if (store == null || store.getMerchantId() == null || !store.getMerchantId().equals(merchantId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "意向单不存在");
        }
        return ApiResponse.ok(toDto(bi, store));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResponse<Void> status(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        access.require(auth, request, false);
        throw new BusinessException(HttpStatus.FORBIDDEN, 403, DENY);
    }

    @RequestMapping(value = "/{id}/notes", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResponse<Void> notes(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        access.require(auth, request, false);
        throw new BusinessException(HttpStatus.FORBIDDEN, 403, DENY);
    }

    private Map<String, Object> toDto(BookingIntent bi, Homestay store) {
        String summary = notes.findFirstByIntentIdOrderByCreatedAtDesc(bi.getId())
                .map(n -> n.getNote() == null ? "" : n.getNote())
                .orElse("");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", bi.getId());
        m.put("intentNo", bi.getIntentNo());
        m.put("storeId", bi.getHomestayId());
        m.put("storeName", store == null ? "" : store.getName());
        m.put("checkIn", bi.getCheckIn().toString());
        m.put("checkOut", bi.getCheckOut().toString());
        m.put("guests", bi.getGuests());
        m.put("status", bi.getStatus());
        m.put("contactName", bi.getContactName());
        m.put("contactPhoneMasked", PhoneMask.mask(bi.getContactPhone()));
        m.put("noteSummary", summary);
        m.put("createdAt", bi.getCreatedAt().toString());
        return m;
    }
}
