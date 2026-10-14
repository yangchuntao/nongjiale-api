package com.nongjiale.web;

import com.nongjiale.domain.BookingIntent;
import com.nongjiale.domain.Homestay;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.service.MerchantApplicationService;
import com.nongjiale.service.PageRequests;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class PortalMeController {
    private final BookingIntentRepository intents;
    private final HomestayRepository homestays;
    private final MerchantApplicationService applications;

    public PortalMeController(BookingIntentRepository intents, HomestayRepository homestays,
                              MerchantApplicationService applications) {
        this.intents = intents;
        this.homestays = homestays;
        this.applications = applications;
    }

    @GetMapping("/intents")
    public ApiResponse<PageResult<Map<String, Object>>> intents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication auth) {
        Long userId = requireTourist(auth).id();
        Page<BookingIntent> result = intents.findByUserIdOrderByCreatedAtDesc(userId, PageRequests.of(page, pageSize));
        return ApiResponse.ok(new PageResult<>(
                result.map(bi -> brief(bi, false)).getContent(), result.getTotalElements()));
    }

    @GetMapping("/intents/{id}")
    public ApiResponse<Map<String, Object>> intent(@PathVariable Long id, Authentication auth) {
        Long userId = requireTourist(auth).id();
        BookingIntent bi = intents.findById(id)
                .filter(row -> userId.equals(row.getUserId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "意向单不存在"));
        return ApiResponse.ok(brief(bi, true));
    }

    @GetMapping("/merchant-application")
    public ApiResponse<Map<String, Object>> merchantApplication(Authentication auth) {
        return ApiResponse.ok(applications.mine(requireTourist(auth).id()));
    }

    private Map<String, Object> brief(BookingIntent bi, boolean detail) {
        Homestay h = homestays.findById(bi.getHomestayId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", bi.getId());
        m.put("intentNo", bi.getIntentNo());
        m.put("homestayId", bi.getHomestayId());
        m.put("homestayName", h == null ? "" : h.getName());
        m.put("checkIn", bi.getCheckIn().toString());
        m.put("checkOut", bi.getCheckOut().toString());
        m.put("guests", bi.getGuests());
        m.put("status", bi.getStatus());
        m.put("createdAt", bi.getCreatedAt().toString());
        if (detail) {
            m.put("contactName", bi.getContactName());
            m.put("contactPhone", bi.getContactPhone());
            m.put("travelMode", bi.getTravelMode());
        }
        return m;
    }

    private static AuthPrincipal requireTourist(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isTourist()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        return p;
    }
}
