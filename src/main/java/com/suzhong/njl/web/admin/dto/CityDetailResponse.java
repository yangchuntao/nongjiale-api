package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.RegionOpenStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 开城详情（{@code GET /api/admin/cities/{cityId}} 的 data，
 * 以及清单修改、开通、关城接口的返回）。
 *
 * <p>在列表字段之外补充阈值、五项清单、能否开通/关城与缺项说明。
 * {@code canOpen} 只在当前未开通且 {@code missingReasons} 为空时为 true；
 * {@code canClose} 只在当前已开通时为 true。计数按实时数据推导，不落库。
 * 城市不存在或不是市返回 404「城市不存在」，节点停用返回 400「地区不可用」。
 *
 * @param id            市 ID
 * @param provinceName  父省名称，可空；父节点不是省时为 null
 * @param name          市名称
 * @param slug          市 slug
 * @param openStatus    对客开通态，取值见 {@link RegionOpenStatus}：OPEN、CLOSED；库内空值按 CLOSED 返回
 * @param storeCount    上架、非测试且地区启用的门店数，含下属区县
 * @param articleCount  已发布攻略数，含下属区县
 * @param updatedAt     清单最近操作时间，可空；没有清单行时为 null
 * @param enabled       地区节点是否可维护；能进入本接口说明一定为 true
 * @param minStores     生效的最低上架店数，见开城阈值配置，默认 5
 * @param minArticles   生效的最低攻略数，见开城阈值配置，默认 1
 * @param checklist     五项勾选状态，②③为实时推导
 * @param canOpen       当前未开通且全部达标时为 true
 * @param canClose      当前已开通时为 true
 * @param missingReasons 未满足项的中文说明，顺序固定；已开通且只是「已经开了」时为空数组
 * @param version       清单版本号；没有清单行时为 0
 * @param updatedBy     最近操作的运营 ID，可空
 */
@Schema(name = "CityDetailResponse", description = "开城详情响应，用于 GET /api/admin/cities/{cityId} 的 data，清单修改、开通、关城接口也返回本结构")
public record CityDetailResponse(
        @Schema(description = "市 ID")
        Long id,
        @Schema(description = "父省名称，可空；父节点不是省时为 null")
        String provinceName,
        @Schema(description = "市名称")
        String name,
        @Schema(description = "市 slug")
        String slug,
        @Schema(description = "对客开通态，取值 OPEN、CLOSED；库内空值按 CLOSED 返回")
        RegionOpenStatus openStatus,
        @Schema(description = "上架、非测试且地区启用的门店数，含下属区县")
        long storeCount,
        @Schema(description = "已发布攻略数，含下属区县")
        long articleCount,
        @Schema(description = "清单最近操作时间，可空；没有清单行时为 null，格式为 Asia/Shanghai 的 ISO-8601")
        LocalDateTime updatedAt,
        @Schema(description = "地区节点是否可维护；能进入本接口说明一定为 true")
        Boolean enabled,
        @Schema(description = "生效的最低上架店数，见开城阈值配置，默认 5")
        int minStores,
        @Schema(description = "生效的最低攻略数，见开城阈值配置，默认 1")
        int minArticles,
        @Schema(description = "五项勾选状态，其中②③为实时推导")
        CityChecklistView checklist,
        @Schema(description = "是否可开通；当前未开通且 missingReasons 为空时为 true")
        boolean canOpen,
        @Schema(description = "是否可关城；当前已开通时为 true")
        boolean canClose,
        @Schema(description = "未满足项的中文说明，顺序固定；已开通且只是「已经开了」时为空数组")
        List<String> missingReasons,
        @Schema(description = "清单版本号；没有清单行时为 0，开城/关城请求必须原样回传")
        long version,
        @Schema(description = "最近操作的运营 ID，可空")
        Long updatedBy) {
}
