package com.nongjiale.web;

import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.service.LicenseMediaService;
import com.nongjiale.service.MerchantApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant-applications")
public class MerchantOnboardingController {
    private final MerchantApplicationService applications;
    private final LicenseMediaService media;

    public MerchantOnboardingController(MerchantApplicationService applications, LicenseMediaService media) {
        this.applications = applications;
        this.media = media;
    }

    public record SubmitReq(
            @NotBlank String subjectName,
            @NotBlank String contactName,
            @NotBlank String phone,
            @NotBlank String proposedStoreName,
            @NotNull Long regionId,
            String description,
            String licenseText,
            List<String> licenseImageUrls
    ) {}

    @PostMapping
    public ApiResponse<Map<String, Object>> submit(@Valid @RequestBody SubmitReq req, Authentication auth) {
        Long userId = touristId(auth);
        return ApiResponse.ok(applications.submit(userId, req.subjectName(), req.contactName(), req.phone(),
                req.proposedStoreName(), req.regionId(), req.description(), req.licenseText(), req.licenseImageUrls()));
    }

    @PostMapping("/media")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(Map.of("url", media.save(file)));
    }

    private static Long touristId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && p.isTourist()) {
            return p.id();
        }
        return null;
    }
}
