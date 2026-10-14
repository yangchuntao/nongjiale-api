package com.nongjiale.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "homestay_room")
public class HomestayRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(name = "room_count")
    private Integer roomCount;
    @Column(name = "price_from", nullable = false)
    private BigDecimal priceFrom;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }
    public Long getHomestayId() { return homestayId; }
    public String getName() { return name; }
    public Integer getRoomCount() { return roomCount; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public int getSortOrder() { return sortOrder; }
}
