package com.nongjiale.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequests {
    private PageRequests() {}

    public static PageRequest of(int page, int pageSize) {
        return of(page, pageSize, Sort.unsorted());
    }

    public static PageRequest of(int page, int pageSize, Sort sort) {
        int p = Math.max(page, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 50);
        return PageRequest.of(p, size, sort);
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
