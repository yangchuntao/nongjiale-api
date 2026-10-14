package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "contact_message")
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "homestay_id")
    private Long homestayId;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "contact_name", nullable = false, length = 64)
    private String contactName;
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getHomestayId() { return homestayId; }
    public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
