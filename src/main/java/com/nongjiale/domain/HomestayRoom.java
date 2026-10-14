package com.nongjiale.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 房型/价格档。商家 API 的 package 落在本表，packageId == id，storeId == homestayId。
 */
@Entity
@Table(name = "homestay_room")
public class HomestayRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(name = "room_count")
    private Integer roomCount;
    @Column(name = "price_from", nullable = false)
    private BigDecimal priceFrom;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }
    public Long getHomestayId() { return homestayId; }
    public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getRoomCount() { return roomCount; }
    public void setRoomCount(Integer roomCount) { this.roomCount = roomCount; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
