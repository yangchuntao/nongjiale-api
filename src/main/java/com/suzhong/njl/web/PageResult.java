package com.suzhong.njl.web;

import java.util.List;

/**
 * 通用分页结果，作为 {@code ApiResponse.data} 的一部分返回。
 *
 * <p>页码从 1 开始，{@code pageSize} 允许 1 到 100；超出范围由分页参数校验直接返回 400。
 * {@code total} 是满足筛选条件的总条数，不是当页条数。
 *
 * @param items    当页数据，无数据时为空列表而不是 null
 * @param total    符合条件的总条数
 * @param page     当前页码，从 1 开始
 * @param pageSize 每页条数
 * @param <T>      元素类型
 */
public record PageResult<T>(List<T> items, long total, int page, int pageSize) {
}
