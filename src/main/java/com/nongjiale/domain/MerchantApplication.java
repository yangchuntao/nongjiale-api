package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "merchant_application")
public class MerchantApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "subject_name", nullable = false, length = 128)
    private String subjectName;
    @Column(name = "contact_name", nullable = false, length = 64)
    private String contactName;
    @Column(nullable = false, length = 20)
    private String phone;
    @Column(name = "proposed_store_name", nullable = false, length = 128)
    private String proposedStoreName;
    @Column(name = "region_id", nullable = false)
    private Long regionId;
    @Column(length = 2000)
    private String description;
    @Column(name = "license_text", length = 2000)
    private String licenseText;
    @Lob
    @Column(name = "license_image_urls")
    private String licenseImageUrls;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;
    @Column(name = "merchant_id")
    private Long merchantId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProposedStoreName() { return proposedStoreName; }
    public void setProposedStoreName(String proposedStoreName) { this.proposedStoreName = proposedStoreName; }
    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLicenseText() { return licenseText; }
    public void setLicenseText(String licenseText) { this.licenseText = licenseText; }
    public String getLicenseImageUrls() { return licenseImageUrls; }
    public void setLicenseImageUrls(String licenseImageUrls) { this.licenseImageUrls = licenseImageUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
