package com.nongjiale.service;

import com.nongjiale.domain.AppUser;
import com.nongjiale.domain.Merchant;
import com.nongjiale.domain.MerchantApplication;
import com.nongjiale.domain.MerchantUser;
import com.nongjiale.repository.AppUserRepository;
import com.nongjiale.repository.MerchantApplicationRepository;
import com.nongjiale.repository.MerchantRepository;
import com.nongjiale.repository.MerchantUserRepository;
import com.nongjiale.repository.RegionRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantApplicationService {
    private final MerchantApplicationRepository applications;
    private final MerchantRepository merchants;
    private final MerchantUserRepository users;
    private final RegionRepository regions;
    private final AppUserRepository appUsers;
    private final PasswordEncoder encoder;
    private final JsonStrings json;

    public MerchantApplicationService(MerchantApplicationRepository applications, MerchantRepository merchants,
                                      MerchantUserRepository users, RegionRepository regions,
                                      AppUserRepository appUsers, PasswordEncoder encoder, JsonStrings json) {
        this.applications = applications;
        this.merchants = merchants;
        this.users = users;
        this.regions = regions;
        this.appUsers = appUsers;
        this.encoder = encoder;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> submit(Long userId, String subjectName, String contactName, String phone,
                                      String proposedStoreName, Long regionId, String description,
                                      String licenseText, List<String> licenseImageUrls) {
        if (regions.findById(regionId).isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "地区不存在");
        }
        if (applications.countByStatusForApplicant("PENDING", phone, userId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "已有待审核的入驻申请");
        }
        if (applications.countByStatusForApplicant("APPROVED", phone, userId) > 0
                || users.findByPhone(phone).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "已入驻");
        }
        MerchantApplication app = new MerchantApplication();
        app.setUserId(userId);
        app.setSubjectName(subjectName.trim());
        app.setContactName(contactName.trim());
        app.setPhone(phone.trim());
        app.setProposedStoreName(proposedStoreName.trim());
        app.setRegionId(regionId);
        app.setDescription(description);
        app.setLicenseText(licenseText);
        app.setLicenseImageUrls(json.write(licenseImageUrls == null ? List.of() : licenseImageUrls));
        app.setStatus("PENDING");
        applications.save(app);
        return Map.of("applicationId", app.getId(), "status", "PENDING");
    }

    public Map<String, Object> mine(Long userId) {
        AppUser user = appUsers.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录"));
        List<MerchantApplication> found = applications.findMine(userId, user.getPhone());
        if (found.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "暂无入驻申请");
        }
        return applicantView(found.get(0));
    }

    public Page<Map<String, Object>> adminList(String status, int page, int pageSize) {
        String st = PageRequests.blankToNull(status);
        Page<MerchantApplication> result = st == null
                ? applications.findAllByOrderByCreatedAtDesc(PageRequests.of(page, pageSize))
                : applications.findByStatusOrderByCreatedAtDesc(st, PageRequests.of(page, pageSize));
        return result.map(this::listView);
    }

    public Map<String, Object> adminDetail(Long id) {
        return adminView(require(id));
    }

    @Transactional
    public Map<String, Object> approve(Long id) {
        MerchantApplication app = require(id);
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "申请已处理");
        }
        if (users.findByPhone(app.getPhone()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "手机号已开通商家账号");
        }
        Merchant merchant = new Merchant();
        merchant.setSubjectName(app.getSubjectName());
        merchant.setEnabled(true);
        merchants.save(merchant);

        String initial = Passwords.initial();
        MerchantUser user = new MerchantUser();
        user.setMerchantId(merchant.getId());
        user.setPhone(app.getPhone());
        user.setPasswordHash(encoder.encode(initial));
        user.setMustChangePassword(true);
        user.setEnabled(true);
        users.save(user);

        app.setStatus("APPROVED");
        app.setMerchantId(merchant.getId());
        app.setReviewedAt(Instant.now());
        applications.save(app);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applicationId", app.getId());
        data.put("status", "APPROVED");
        data.put("merchantId", merchant.getId());
        data.put("loginPhone", user.getPhone());
        data.put("initialPassword", initial);
        data.put("mustChangePassword", true);
        return data;
    }

    @Transactional
    public Map<String, Object> reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "驳回原因必填");
        }
        MerchantApplication app = require(id);
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "申请已处理");
        }
        app.setStatus("REJECTED");
        app.setRejectReason(reason.trim());
        app.setReviewedAt(Instant.now());
        return adminView(app);
    }

    private MerchantApplication require(Long id) {
        return applications.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "申请不存在"));
    }

    private Map<String, Object> listView(MerchantApplication app) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("applicationId", app.getId());
        m.put("subjectName", app.getSubjectName());
        m.put("contactName", app.getContactName());
        m.put("phone", app.getPhone());
        m.put("proposedStoreName", app.getProposedStoreName());
        m.put("regionId", app.getRegionId());
        m.put("status", app.getStatus());
        m.put("createdAt", app.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> applicantView(MerchantApplication app) {
        Map<String, Object> m = listView(app);
        m.put("description", app.getDescription());
        m.put("licenseText", app.getLicenseText());
        m.put("licenseImageUrls", json.read(app.getLicenseImageUrls()));
        m.put("rejectReason", app.getRejectReason());
        return m;
    }

    private Map<String, Object> adminView(MerchantApplication app) {
        Map<String, Object> m = applicantView(app);
        m.put("userId", app.getUserId());
        m.put("merchantId", app.getMerchantId());
        m.put("reviewedAt", app.getReviewedAt() == null ? null : app.getReviewedAt().toString());
        return m;
    }
}
