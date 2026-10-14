package com.nongjiale.web;

import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.ContactMessageRepository;
import com.nongjiale.repository.ListingApplicationRepository;
import com.nongjiale.repository.MerchantApplicationRepository;
import com.nongjiale.security.AuthPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final BookingIntentRepository intents;
    private final ContactMessageRepository messages;
    private final MerchantApplicationRepository merchantApplications;
    private final ListingApplicationRepository listingApplications;

    public AdminDashboardController(BookingIntentRepository intents, ContactMessageRepository messages,
                                    MerchantApplicationRepository merchantApplications,
                                    ListingApplicationRepository listingApplications) {
        this.intents = intents;
        this.messages = messages;
        this.merchantApplications = merchantApplications;
        this.listingApplications = listingApplications;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard(Authentication auth) {
        long pendingIntents = intents.findByStatus("PENDING", PageRequest.of(0, 1)).getTotalElements();
        long pendingMessages = messages.findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(0, 1)).getTotalElements();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingIntents", pendingIntents);
        data.put("pendingMessages", pendingMessages);
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && p.isOperator()) {
            data.put("pendingMerchantApplications", merchantApplications.countByStatus("PENDING"));
            data.put("pendingListingApplications", listingApplications.countByStatus("PENDING"));
        }
        return ApiResponse.ok(data);
    }
}
