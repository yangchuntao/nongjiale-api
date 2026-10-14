package com.nongjiale.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 门户/运营的房源。v1.1 商家 API 的门店就是本表：storeId == id。
 */
@Entity
@Table(name = "homestay")
public class Homestay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "merchant_id")
    private Long merchantId;
    @Column(name = "region_id", nullable = false)
    private Long regionId;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(nullable = false, length = 256)
    private String address;
    @Column(length = 32)
    private String phone;
    @Column(length = 2000)
    private String summary;
    @Column(name = "cover_url", length = 512)
    private String coverUrl;
    @Lob
    private String gallery;
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
    @Lob
    private String traffic;
    @Lob
    private String surroundings;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getGallery() { return gallery; }
    public void setGallery(String gallery) { this.gallery = gallery; }
    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
    public String getTraffic() { return traffic; }
    public void setTraffic(String traffic) { this.traffic = traffic; }
    public String getSurroundings() { return surroundings; }
    public void setSurroundings(String surroundings) { this.surroundings = surroundings; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isOnSale() { return "ON_SALE".equals(status); }
}
