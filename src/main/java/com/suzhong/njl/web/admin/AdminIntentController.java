package com.suzhong.njl.web.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.service.admin.AdminIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminIntentDetail;
import com.suzhong.njl.web.admin.dto.AdminIntentItem;
import com.suzhong.njl.web.admin.dto.IntentNoteRequest;
import com.suzhong.njl.web.admin.dto.IntentStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/intents")
public class AdminIntentController {

    private final AdminIntentService adminIntentService;

    public AdminIntentController(AdminIntentService adminIntentService) {
        this.adminIntentService = adminIntentService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminIntentItem>> list(
            @RequestParam(required = false) IntentStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) LocalDate checkInFrom,
            @RequestParam(required = false) LocalDate checkInTo,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminIntentService.list(
                status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo, page, pageSize));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) IntentStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) LocalDate checkInFrom,
            @RequestParam(required = false) LocalDate checkInTo,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo) {
        byte[] body = adminIntentService.export(
                status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo);
        ContentDisposition disposition = ContentDisposition.attachment().filename("intents.csv").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminIntentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminIntentService.detail(id));
    }

    @PostMapping("/{id:\\d+}/status")
    public ApiResponse<AdminIntentDetail> changeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody IntentStatusRequest request) {
        return ApiResponse.ok(adminIntentService.changeStatus(AdminPrincipal.adminId(authentication), id, request));
    }

    @PostMapping("/{id:\\d+}/notes")
    public ApiResponse<AdminIntentDetail> addNote(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody IntentNoteRequest request) {
        return ApiResponse.ok(adminIntentService.addNote(AdminPrincipal.adminId(authentication), id, request));
    }
}
