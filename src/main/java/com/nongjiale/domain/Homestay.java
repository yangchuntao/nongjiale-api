package com.nongjiale.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "homestay")
public class Homestay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "region_id", nullable = false)
    private Long regionId;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(nullable = false, length = 256)
    private String address;
    @Column(name = "cover_url", length = 512)
    private String coverUrl;
    @Column(name = "sales_count", nullable = false)
    private int salesCount;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "price_from")
    private BigDecimal priceFrom;
    @Lob
    private String facilities;
    @Lob
    private String notice;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public Long getRegionId() { return regionId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCoverUrl() { return coverUrl; }
    public int getSalesCount() { return salesCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public String getFacilities() { return facilities; }
    public String getNotice() { return notice; }
    public Long getVersion() { return version; }
    public boolean isOnSale() { return "ON_SALE".equals(status); }
}
