package com.suzhong.njl.service.support;

import org.springframework.http.HttpStatus;

import com.suzhong.njl.web.ApiException;

/**
 * 门户与运营分页参数的统一收口（page 从 1 开始）。
 *
 * <p>职责：把外部传入的可空页码/页大小规范化成合法值，并在越界时直接抛 400，
 * 避免各服务各自写一遍校验、也避免把非法值拼进 SQL 的 LIMIT/OFFSET。
 *
 * @param page     页码，从 1 开始
 * @param pageSize 每页条数，上限 100
 */
public record PageQuery(int page, int pageSize) {

    /**
     * 规范化分页参数。
     *
     * <p>流程：空值取默认（page=1、pageSize=20）→ 校验 page ≥ 1、pageSize 在 1 到 100 之间。
     *
     * @param page     页码，null 表示未传
     * @param pageSize 每页条数，null 表示未传
     * @return 合法的分页对象
     * @throws ApiException 页码小于 1、页大小小于 1 或超过 100 时 400「分页参数不正确」
     */
    public static PageQuery of(Integer page, Integer pageSize) {
        int resolvedPage = page == null ? 1 : page;
        int resolvedSize = pageSize == null ? 20 : pageSize;
        if (resolvedPage < 1 || resolvedSize < 1 || resolvedSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "分页参数不正确");
        }
        return new PageQuery(resolvedPage, resolvedSize);
    }

    /** 计算 SQL OFFSET：{@code (page - 1) * pageSize}，参数已在工厂方法里校验过。 */
    public int offset() {
        return (page - 1) * pageSize;
    }
}
