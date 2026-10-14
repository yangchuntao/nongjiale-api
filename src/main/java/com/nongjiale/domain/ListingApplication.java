package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "listing_application")
public class ListingApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;
    @Column(nullable = false, length = 16)
    private String action;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(length = 1000)
    private String note;
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public Long getId() { return id; }
    public Long getHomestayId() { return homestayId; }
    public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
