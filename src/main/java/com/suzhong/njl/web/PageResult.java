package com.suzhong.njl.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

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
// 同 ApiResponse：泛型 record 不能固定 schema 名，否则不同元素类型会共用同一个 schema。
@Schema(description = "通用分页结果，作为 ApiResponse.data 的一部分返回；页码从 1 开始，超出范围由分页参数校验返回 400")
public record PageResult<T>(
        @Schema(description = "当页数据，无数据时为空列表而不是 null")
        List<T> items,
        @Schema(description = "满足筛选条件的总条数，不是当页条数")
        long total,
        @Schema(description = "当前页码，从 1 开始", example = "1")
        int page,
        @Schema(description = "每页条数，默认 20，允许 1 到 100", example = "20")
        int pageSize) {
}
