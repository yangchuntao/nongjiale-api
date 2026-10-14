package com.nongjiale.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;
    @Column(nullable = false, length = 32)
    private String role;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
