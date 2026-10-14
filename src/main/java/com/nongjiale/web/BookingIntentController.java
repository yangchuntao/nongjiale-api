package com.nongjiale.web;

import com.nongjiale.domain.BookingIntent;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.service.BookingIntentService;
import com.nongjiale.service.SiteConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/booking-intents")
public class BookingIntentController {
    private final BookingIntentService service;
    private final SiteConfigService siteConfig;

    public BookingIntentController(BookingIntentService service, SiteConfigService siteConfig) {
        this.service = service;
        this.siteConfig = siteConfig;
    }

    public record CreateReq(
            @NotNull Long homestayId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int guests,
            String travelMode,
            @NotBlank String contactName,
            @NotBlank String contactPhone
    ) {}

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        AuthPrincipal p = requireTourist(auth);
        BookingIntent bi = service.create(p.id(), req.homestayId(), req.checkIn(), req.checkOut(),
                req.guests(), req.travelMode(), req.contactName(), req.contactPhone());
        return ApiResponse.ok(Map.of(
                "intentId", bi.getId(),
                "intentNo", bi.getIntentNo(),
                "status", bi.getStatus(),
                "contactPhone", siteConfig.contactPhone()
        ));
    }

    private static AuthPrincipal requireTourist(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isTourist()) {
            throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        return p;
    }
}
