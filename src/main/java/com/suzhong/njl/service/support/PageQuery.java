package com.suzhong.njl.service.support;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

public record PageQuery(int page, int pageSize) {

    public static PageQuery of(Integer page, Integer pageSize) {
        int resolvedPage = page == null ? 1 : page;
        int resolvedSize = pageSize == null ? 20 : pageSize;
        if (resolvedPage < 1 || resolvedSize < 1 || resolvedSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "分页参数不正确");
        }
        return new PageQuery(resolvedPage, resolvedSize);
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
