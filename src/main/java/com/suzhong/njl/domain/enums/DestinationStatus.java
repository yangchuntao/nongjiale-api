package com.suzhong.njl.domain.enums;

/**
 * 门户房源列表对所选地区的开通判断。不是 region.open_status 的落库值。
 *
 * <p>只出现在 {@code GET /api/stores} 响应的 {@code destinationStatus} 字段，
 * 供前端决定是否展示「该目的地暂未开通」空态。
 * <ul>
 *   <li>{@code OPEN}：所选市已开通，或区县所属市已开通；</li>
 *   <li>{@code CLOSED}：所选市未开通，列表为空且带空态提示；</li>
 *   <li>{@code NONE}：未传地区（全站）或传的是省，只做范围收敛，不判定目的地开通与否。</li>
 * </ul>
 */
public enum DestinationStatus {
    /** 目的地已开通，列表正常返回该范围内的上架门店。 */
    OPEN,
    /** 目的地未开通，返回空列表并提示「该目的地暂未开通」。 */
    CLOSED,
    /** 不针对具体市（全站或省级查询），不展示未开通空态。 */
    NONE
}
