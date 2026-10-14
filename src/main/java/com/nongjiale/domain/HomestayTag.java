package com.nongjiale.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "homestay_tag")
public class HomestayTag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    @Column(nullable = false, length = 64)
    private String tag;

    public Long getId() { return id; }
    public Long getHomestayId() { return homestayId; }
    public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
