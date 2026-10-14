package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "intent_note")
public class IntentNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "intent_id", nullable = false)
    private Long intentId;
    @Column(name = "admin_id", nullable = false)
    private Long adminId;
    @Column(nullable = false, length = 32)
    private String action;
    @Column(length = 1000)
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
