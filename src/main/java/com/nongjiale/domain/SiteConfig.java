package com.nongjiale.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "site_config")
public class SiteConfig {
    @Id
    private Long id;
    @Column(name = "site_name", nullable = false, length = 128)
    private String siteName = "";
    @Column(name = "primary_color", nullable = false, length = 16)
    private String primaryColor = "";
    @Column(name = "contact_phone", nullable = false, length = 32)
    private String contactPhone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
