package com.nongjiale.web;

import com.nongjiale.domain.ContactMessage;
import com.nongjiale.repository.ContactMessageRepository;
import com.nongjiale.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact-messages")
public class ContactMessageController {
    private final ContactMessageRepository messages;

    @Value("${nongjiale.contact-phone}")
    private String contactPhone;

    public ContactMessageController(ContactMessageRepository messages) {
        this.messages = messages;
    }

    public record CreateReq(Long homestayId, @NotBlank String content,
                            @NotBlank String contactName, @NotBlank String contactPhone) {}

    @PostMapping
    @Transactional
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isTourist()) {
            throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        ContactMessage m = new ContactMessage();
        m.setUserId(p.id());
        m.setHomestayId(req.homestayId());
        m.setContent(req.content());
        m.setContactName(req.contactName());
        m.setContactPhone(req.contactPhone());
        m.setStatus("PENDING");
        messages.save(m);
        return ApiResponse.ok(Map.of(
                "messageId", m.getId(),
                "status", m.getStatus(),
                "contactPhone", contactPhone
        ));
    }
}
