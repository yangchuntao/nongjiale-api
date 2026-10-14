package com.nongjiale.web;

import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.ListingApplicationRepository;
import com.nongjiale.service.MerchantAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/dashboard")
public class MerchantDashboardController {
    private final MerchantAccess access;
    private final HomestayRepository homestays;
    private final ListingApplicationRepository listings;
    private final BookingIntentRepository intents;

    public MerchantDashboardController(MerchantAccess access, HomestayRepository homestays,
                                       ListingApplicationRepository listings, BookingIntentRepository intents) {
        this.access = access;
        this.homestays = homestays;
        this.listings = listings;
        this.intents = intents;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard(Authentication auth, HttpServletRequest request) {
        MerchantAccess.Context ctx = access.require(auth, request, false);
        Long merchantId = ctx.merchant().getId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("storeCount", homestays.countByMerchantId(merchantId));
        data.put("pendingListingApplications", listings.countByMerchantIdAndStatus(merchantId, "PENDING"));
        data.put("newIntentCount", intents.countPendingForMerchant(merchantId));
        return ApiResponse.ok(data);
    }
}
