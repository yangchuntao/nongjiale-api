package com.suzhong.njl.web.portal.dto;

import java.util.List;

import com.suzhong.njl.domain.enums.DestinationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 门店列表（{@code GET /api/stores} 的响应体，公开接口）。
 *
 * <p>只列出已开通市里的上架门店。未开通市（或区县所属市未开通）返回 HTTP 200 + 空列表 +
 * {@code destinationStatus=CLOSED} + {@code emptyHint}「该目的地暂未开通」，不抛异常；
 * 未传地区或传省时只做范围收敛，{@code destinationStatus=NONE}，不展示未开通空态。
 *
 * @param items             当页门店卡片，无数据时为空列表而不是 null
 * @param total             满足筛选条件的总条数（不是当页条数）；CLOSED 空页固定为 0
 * @param page              当前页码，从 1 开始；未传时默认 1
 * @param pageSize          每页条数；未传时默认 20，允许 1 到 100，越界返回 400
 * @param destinationStatus 目的地状态，见 {@link com.suzhong.njl.domain.enums.DestinationStatus}：
 *                          {@code OPEN} 已开通、{@code CLOSED} 未开通（空列表）、{@code NONE} 全站或省级查询不判定
 * @param emptyHint         空态提示文案，仅 {@code CLOSED} 时为「该目的地暂未开通」，其余为 null
 */
@Schema(name = "StoreListResponse", description = "门店列表，GET /api/stores 的响应体；未开通市返回 HTTP 200 + 空列表 + destinationStatus=CLOSED，不抛异常")
public record StoreListResponse(
        @Schema(description = "当页门店卡片，无数据时为空列表而不是 null")
        List<StoreCard> items,
        @Schema(description = "满足筛选条件的总条数（不是当页条数）；CLOSED 空页固定为 0")
        long total,
        @Schema(description = "当前页码，从 1 开始；未传时默认 1", example = "1")
        int page,
        @Schema(description = "每页条数；未传时默认 20，允许 1 到 100，越界返回 400", example = "20")
        int pageSize,
        @Schema(description = "目的地状态：OPEN 已开通、CLOSED 未开通（空列表）、NONE 全站或省级查询不判定")
        DestinationStatus destinationStatus,
        @Schema(description = "空态提示文案，仅 CLOSED 时为「该目的地暂未开通」，其余为 null")
        String emptyHint) {
}
