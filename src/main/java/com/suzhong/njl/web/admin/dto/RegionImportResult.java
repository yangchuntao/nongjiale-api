package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 区划导入结果（运营端）。
 *
 * @param total    本次处理的数据行数
 * @param inserted 新增行数
 * @param updated  更新行数（名称/排序/经纬度有变化）
 * @param skipped  跳过行数（已存在且无变化）
 * @param message  结果说明，中文
 */
@Schema(name = "RegionImportResult", description = "区划导入结果统计")
public record RegionImportResult(
        @Schema(description = "本次处理的数据行数", example = "3243")
        int total,
        @Schema(description = "新增行数", example = "3230")
        int inserted,
        @Schema(description = "更新行数", example = "0")
        int updated,
        @Schema(description = "跳过行数（已存在且无变化）", example = "13")
        int skipped,
        @Schema(description = "结果说明", example = "区划同步完成")
        String message) {
}
