package com.nongjiale.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "region")
public class Region {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String slug;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public boolean isEnabled() { return enabled; }
}
