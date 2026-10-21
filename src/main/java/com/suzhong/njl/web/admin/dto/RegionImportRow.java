package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 区划导入的一行输入（运营端）。
 *
 * <p>字段与 {@code db/region/regions.csv} 的列一一对应，也作为后续上传 CSV 或对接其他数据源时的通用入参。
 *
 * @param adcode       国家标准行政区划代码，如 {@code 320500}；必填，全局唯一，导入幂等键
 * @param name         地区全称，如「苏州市」；必填
 * @param level        行政层级数字码：1 省、2 市、3 区县；必填
 * @param parentAdcode 上级行政区划代码，省级留空
 * @param sortOrder    同级排序号，升序
 * @param centerLng    政府驻地经度，可空
 * @param centerLat    政府驻地纬度，可空
 */
@Schema(name = "RegionImportRow", description = "区划导入行，字段对齐 region CSV 列")
public record RegionImportRow(
        @Schema(description = "国家标准行政区划代码，全局唯一，导入幂等键", example = "320500")
        String adcode,
        @Schema(description = "地区全称", example = "苏州市")
        String name,
        @Schema(description = "行政层级数字码：1 省、2 市、3 区县", example = "2")
        int level,
        @Schema(description = "上级行政区划代码，省级留空", example = "320000")
        String parentAdcode,
        @Schema(description = "同级排序号，升序", example = "500")
        int sortOrder,
        @Schema(description = "政府驻地经度，可空")
        String centerLng,
        @Schema(description = "政府驻地纬度，可空")
        String centerLat) {
}
