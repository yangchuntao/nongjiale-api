package com.nongjiale.web;

import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.ContactMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final BookingIntentRepository intents;
    private final ContactMessageRepository messages;

    public AdminDashboardController(BookingIntentRepository intents, ContactMessageRepository messages) {
        this.intents = intents;
        this.messages = messages;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard() {
        long pendingIntents = intents.findByStatus("PENDING", PageRequest.of(0, 1)).getTotalElements();
        long pendingMessages = messages.findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(0, 1)).getTotalElements();
        return ApiResponse.ok(Map.of(
                "pendingIntents", pendingIntents,
                "pendingMessages", pendingMessages
        ));
    }
}
