package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.BookingIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.CreateIntentRequest;
import com.suzhong.njl.web.portal.dto.IntentResponse;

import jakarta.validation.Valid;

@RestController
public class BookingIntentController {

    private final BookingIntentService bookingIntentService;

    public BookingIntentController(BookingIntentService bookingIntentService) {
        this.bookingIntentService = bookingIntentService;
    }

    @PostMapping("/api/booking-intents")
    public ApiResponse<IntentResponse> create(Authentication authentication, @Valid @RequestBody CreateIntentRequest request) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.create(user, request));
    }

    @GetMapping("/api/me/intents")
    public ApiResponse<PageResult<IntentResponse>> mine(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.mine(user, page, pageSize));
    }

    @GetMapping("/api/me/intents/{id}")
    public ApiResponse<IntentResponse> mineOne(Authentication authentication, @PathVariable Long id) {
        AuthenticatedUser user = CurrentUser.require(authentication, UserType.TOURIST);
        return ApiResponse.ok(bookingIntentService.mineOne(user, id));
    }
}
