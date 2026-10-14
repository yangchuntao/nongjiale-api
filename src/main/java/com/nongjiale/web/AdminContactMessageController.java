package com.nongjiale.web;

import com.nongjiale.domain.ContactMessage;
import com.nongjiale.repository.ContactMessageRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contact-messages")
public class AdminContactMessageController {
    private final ContactMessageRepository messages;

    public AdminContactMessageController(ContactMessageRepository messages) {
        this.messages = messages;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int p = Math.max(page, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 50);
        Page<ContactMessage> result = (status == null || status.isBlank())
                ? messages.findAll(PageRequest.of(p, size))
                : messages.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(p, size));
        return ApiResponse.ok(new PageResult<>(result.map(this::dto).getContent(), result.getTotalElements()));
    }

    public record DoneReq(@NotBlank String note) {}

    @PostMapping("/{id}/done")
    @Transactional
    public ApiResponse<Map<String, Object>> markDone(@PathVariable Long id, @Valid @RequestBody DoneReq req) {
        ContactMessage m = messages.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "留言不存在"));
        m.setStatus("DONE");
        return ApiResponse.ok(dto(m));
    }

    private Map<String, Object> dto(ContactMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("userId", m.getUserId());
        map.put("homestayId", m.getHomestayId());
        map.put("content", m.getContent());
        map.put("contactName", m.getContactName());
        map.put("contactPhone", m.getContactPhone());
        map.put("status", m.getStatus());
        map.put("createdAt", m.getCreatedAt().toString());
        return map;
    }
}
