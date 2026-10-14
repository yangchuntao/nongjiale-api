package com.nongjiale.web;

import com.nongjiale.domain.BookingIntent;
import com.nongjiale.domain.IntentNote;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.service.BookingIntentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/intents", "/api/admin/booking-intents"})
public class AdminIntentController {
    private final BookingIntentRepository intents;
    private final BookingIntentService service;

    public AdminIntentController(BookingIntentRepository intents, BookingIntentService service) {
        this.intents = intents;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int p = Math.max(page, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 50);
        Page<BookingIntent> result = (status == null || status.isBlank())
                ? intents.findAllByOrderByCreatedAtDesc(PageRequest.of(p, size))
                : intents.findByStatus(status, PageRequest.of(p, size));
        return ApiResponse.ok(new PageResult<>(result.map(this::brief).getContent(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        BookingIntent bi = intents.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "意向单不存在"));
        Map<String, Object> body = new HashMap<>(brief(bi));
        List<Map<String, Object>> timeline = service.notes(id).stream().map(this::noteDto).toList();
        body.put("notes", timeline);
        return ApiResponse.ok(body);
    }

    public record UpdateStatusReq(@NotBlank String status, String note, Long version) {}

    @PostMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateStatusReq req,
                                                         Authentication auth) {
        AuthPrincipal p = requireAdmin(auth);
        BookingIntent bi = service.updateStatus(id, p.id(), req.status(), req.note(), req.version());
        return ApiResponse.ok(brief(bi));
    }

    private Map<String, Object> brief(BookingIntent bi) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", bi.getId());
        m.put("intentNo", bi.getIntentNo());
        m.put("userId", bi.getUserId());
        m.put("homestayId", bi.getHomestayId());
        m.put("checkIn", bi.getCheckIn().toString());
        m.put("checkOut", bi.getCheckOut().toString());
        m.put("guests", bi.getGuests());
        m.put("contactName", bi.getContactName());
        m.put("contactPhone", bi.getContactPhone());
        m.put("status", bi.getStatus());
        m.put("version", bi.getVersion());
        m.put("createdAt", bi.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> noteDto(IntentNote n) {
        return Map.of(
                "id", n.getId(),
                "adminId", n.getAdminId(),
                "action", n.getAction(),
                "note", n.getNote() == null ? "" : n.getNote(),
                "createdAt", n.getCreatedAt().toString()
        );
    }

    private static AuthPrincipal requireAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isAdmin()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        return p;
    }
}
