package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "booking_intent")
public class BookingIntent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "intent_no", nullable = false, unique = true, length = 32)
    private String intentNo;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;
    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;
    @Column(nullable = false)
    private int guests;
    @Column(name = "travel_mode", length = 64)
    private String travelMode;
    @Column(name = "contact_name", nullable = false, length = 64)
    private String contactName;
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public String getIntentNo() { return intentNo; }
    public void setIntentNo(String intentNo) { this.intentNo = intentNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getHomestayId() { return homestayId; }
    public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public String getTravelMode() { return travelMode; }
    public void setTravelMode(String travelMode) { this.travelMode = travelMode; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
