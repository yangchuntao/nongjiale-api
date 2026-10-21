package com.suzhong.njl.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionSource;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.RegionImportResult;
import com.suzhong.njl.web.admin.dto.RegionImportRow;

/**
 * 全国行政区划导入服务（运营端）。
 *
 * <p>职责：把打包在 jar 内的 {@code db/region/regions.csv}（省市区三级全量，约 3200 行）导入 {@code region} 表，
 * 让运营不写迁移脚本也能在开城列表看到全国所有地级市。
 *
 * <p>数据来源：CSV 由 {@code tools/region_sync/fetch_regions.py} 从公开行政区划边界服务抓取属性字段生成；
 * 更新区划时重跑脚本、替换资源文件、重新打包即可，不涉及 DDL。
 *
 * <p>关键约定：
 * <ul>
 *   <li><b>幂等</b>：以 {@code adcode} 为键，已存在只补区划属性，重复执行结果一致；</li>
 *   <li><b>不覆盖运营配置</b>：更新已有行绝不触碰 {@code open_status}、{@code enabled}、{@code parent_id}，
 *       避免一次区划同步把已开通城市打回未开通、或把运营停用的地区重新启用；</li>
 *   <li><b>两遍插入</b>：先按层级升序插入省，再市，再区县，保证父级 ID 一定已存在，
 *       不依赖 CSV 的行顺序；</li>
 *   <li><b>slug 不重算</b>：slug 全局唯一且门户路由在用，新行用 {@code ad<adcode>} 生成并避让冲突，
 *       已有行保持原 slug，{@code /yangzhou} 这类老路由不会失效；</li>
 *   <li>整个导入单事务，任一行校验失败全部回滚，不留半套数据。</li>
 * </ul>
 */
@Service
public class RegionImportService {

    /** 打包在 jar 内的全国区划数据。放在 db/region 而不是 db/migration，避免被 Flyway 当成迁移脚本执行。 */
    private static final String RESOURCE = "db/region/regions.csv";

    /** CSV 表头，顺序必须与抓取脚本输出一致；顺序不符直接拒绝，防止列错位写库。 */
    private static final List<String> HEADER = List.of(
            "adcode", "name", "level", "parent_adcode", "sort_order", "center_lng", "center_lat");

    private final RegionMapper regionMapper;

    public RegionImportService(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    /**
     * 把 jar 内置的全国区划数据同步进库。
     *
     * @return 导入统计：总行数、新增、更新、跳过
     * @throws ApiException 资源缺失、表头不符或某行解析失败时 500；数据本身不合法时 400
     */
    @Transactional
    public RegionImportResult syncFromResource() {
        return apply(read());
    }

    /**
     * 导入调用方给定的区划行，供后续上传 CSV 或对接其他数据源复用。
     *
     * <p>流程：校验 → 建 adcode 索引 → 按层级升序逐行插入或更新。
     *
     * @param rows 区划行，顺序任意，内部按层级排序后处理
     * @return 导入统计
     * @throws ApiException 行数据非法时 400，消息指出第一个问题
     */
    @Transactional
    public RegionImportResult apply(List<RegionImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "区划数据为空");
        }
        validate(rows);

        Map<String, Region> existingByAdcode = indexByAdcode();
        Map<String, Region> existingBySlug = indexBySlug();

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        // 按层级升序：先省、再市、再区县，确保插入子级时父级 ID 已落库
        List<RegionImportRow> ordered = new ArrayList<>(rows);
        ordered.sort(java.util.Comparator.comparingInt(RegionImportRow::level));

        for (RegionImportRow row : ordered) {
            Region current = existingByAdcode.get(row.adcode());
            if (current == null) {
                Region created = buildNew(row, existingBySlug);
                regionMapper.insert(created);
                existingByAdcode.put(row.adcode(), created);
                inserted++;
                continue;
            }
            if (needsUpdate(current, row)) {
                // 只写区划属性；open_status / enabled / parent_id 一律保留库内原值
                current.setName(row.name());
                current.setShortName(shortName(row.name()));
                current.setSortOrder(row.sortOrder());
                current.setCenterLng(toDecimal(row.centerLng()));
                current.setCenterLat(toDecimal(row.centerLat()));
                regionMapper.updateById(current);
                updated++;
            } else {
                skipped++;
            }
        }
        return new RegionImportResult(rows.size(), inserted, updated, skipped, "区划同步完成");
    }

    /**
     * 读取内置 CSV 并解析。
     *
     * <p>公开给测试与后续「导入预览」接口复用：预览只需解析、不写库。
     *
     * @return 区划行，按 CSV 原顺序
     * @throws ApiException 资源缺失、表头不符或行解析失败时 500
     */
    public List<RegionImportRow> read() {
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "区划数据文件不可用");
        }
        try (InputStream in = resource.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "区划数据文件为空");
            }
            List<String> header = List.of(dropBom(headerLine).split(",", -1));
            if (!header.equals(HEADER)) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "区划数据文件表头不符");
            }
            List<RegionImportRow> rows = new ArrayList<>();
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parse(line, lineNo));
            }
            return rows;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** 解析一行；列数不足、层级或排序非数字直接拒绝，脏数据不落库。 */
    private RegionImportRow parse(String line, int lineNo) {
        String[] cells = line.split(",", -1);
        if (cells.length < HEADER.size()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "区划数据第 " + lineNo + " 行列数不足");
        }
        int level = parseIntOrFail(cells[2], lineNo, "层级");
        int sortOrder = parseIntOrFail(cells[4], lineNo, "排序");
        // 先过一遍枚举，非法层级在这里就失败，不留到写库时抛异常
        RegionLevel.fromCode(level);
        return new RegionImportRow(
                cells[0].trim(),
                cells[1].trim(),
                level,
                cells[3].trim(),
                sortOrder,
                emptyToNull(cells[5]),
                emptyToNull(cells[6]));
    }

    private int parseIntOrFail(String raw, int lineNo, String field) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "区划数据第 " + lineNo + " 行" + field + "非法");
        }
    }

    /**
     * 校验：编码唯一、名称非空、层级连续、父级存在。
     *
     * <p>省级必须无父级；市级父级必须是省；区县父级必须是市。直辖市与港澳的区县直接挂省，
     * 由抓取脚本合成市级节点后不存在这种结构，所以这里严格要求差 1，不放开层级跳跃。
     */
    private void validate(List<RegionImportRow> rows) {
        Map<String, RegionImportRow> byAdcode = new HashMap<>();
        for (RegionImportRow row : rows) {
            if (row.adcode() == null || row.adcode().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "存在缺少行政区划代码的行");
            }
            if (row.name() == null || row.name().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "区划 " + row.adcode() + " 缺少名称");
            }
            if (byAdcode.put(row.adcode(), row) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "行政区划代码重复：" + row.adcode());
            }
        }
        for (RegionImportRow row : rows) {
            RegionLevel level = RegionLevel.fromCode(row.level());
            String parent = row.parentAdcode();
            boolean root = parent == null || parent.isBlank();
            if (root) {
                if (level != RegionLevel.PROVINCE) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "区划 " + row.adcode() + " 非省级却没有父级");
                }
                continue;
            }
            RegionImportRow parentRow = byAdcode.get(parent);
            if (parentRow == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "区划 " + row.adcode() + " 的父级 " + parent + " 不在本次数据中");
            }
            if (parentRow.level() != level.code() - 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "区划 " + row.adcode() + " 与其父级 " + parent + " 层级不连续");
            }
        }
    }

    /** 按 adcode 建索引，跳过没有 adcode 的存量种子行。 */
    private Map<String, Region> indexByAdcode() {
        Map<String, Region> result = new HashMap<>();
        for (Region region : regionMapper.selectList(Wrappers.<Region>lambdaQuery())) {
            if (region.getAdcode() != null && !region.getAdcode().isBlank()) {
                result.put(region.getAdcode(), region);
            }
        }
        return result;
    }

    /** 按 slug 建索引，用于新插入行避让 slug 冲突。 */
    private Map<String, Region> indexBySlug() {
        Map<String, Region> result = new HashMap<>();
        for (Region region : regionMapper.selectList(Wrappers.<Region>lambdaQuery())) {
            result.put(region.getSlug(), region);
        }
        return result;
    }

    /** 组装新行：adcode、来源 SYNC、默认启用；父级按 adcode 查库内 ID。 */
    private Region buildNew(RegionImportRow row, Map<String, Region> existingBySlug) {
        Region region = new Region();
        region.setAdcode(row.adcode());
        region.setName(row.name());
        region.setShortName(shortName(row.name()));
        region.setLevel(RegionLevel.fromCode(row.level()));
        region.setSortOrder(row.sortOrder());
        region.setEnabled(Boolean.TRUE);
        region.setSource(RegionSource.SYNC.code());
        region.setCenterLng(toDecimal(row.centerLng()));
        region.setCenterLat(toDecimal(row.centerLat()));
        region.setSlug(uniqueSlug(row.adcode(), existingBySlug));
        String parent = row.parentAdcode();
        if (parent != null && !parent.isBlank()) {
            List<Region> parents = regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getAdcode, parent));
            if (!parents.isEmpty()) {
                region.setParentId(parents.get(0).getId());
            }
        }
        return region;
    }

    /**
     * 用行政区划代码生成 slug，避免依赖中文转拼音。
     *
     * <p>形如 {@code ad320500}；与现存 slug 冲突时追加序号。已有行不改名，
     * 门户上已发出去的 {@code /yangzhou} 这类路由不受影响。
     */
    private String uniqueSlug(String adcode, Map<String, Region> existingBySlug) {
        String base = "ad" + adcode;
        String candidate = base;
        int suffix = 2;
        while (existingBySlug.containsKey(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        existingBySlug.put(candidate, new Region());
        return candidate;
    }

    /** 名称、排序、经纬度任一不同就需要更新；只比较区划属性，不看开城态与启停。 */
    private boolean needsUpdate(Region current, RegionImportRow row) {
        return !row.name().equals(current.getName())
                || !Integer.valueOf(row.sortOrder()).equals(current.getSortOrder())
                || !sameDecimal(toDecimal(row.centerLng()), current.getCenterLng())
                || !sameDecimal(toDecimal(row.centerLat()), current.getCenterLat());
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private BigDecimal toDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 由全称推导简称：去掉末尾的省/市/自治区/自治州/地区等后缀。
     * 只用于展示，不参与业务判断；推导不出就返回原名。
     */
    public static String shortName(String name) {
        if (name == null) {
            return null;
        }
        String[] suffixes = {"特别行政区", "维吾尔自治区", "壮族自治区", "回族自治区",
            "自治区", "自治州", "地区", "省", "市", "盟"};
        for (String suffix : suffixes) {
            if (name.length() > suffix.length() && name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String dropBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }

    /**
     * 按父级 adcode 分组，供运营端分区划浏览（省 → 其下市 → 其下区县）。
     *
     * @return 父级 adcode 到子级列表的映射，子级按 {@code sortOrder} 升序
     */
    public Map<String, List<Region>> childrenByParent() {
        List<Region> all = regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .orderByAsc(Region::getSortOrder)
                .orderByAsc(Region::getId));
        Map<Long, String> adcodeById = new HashMap<>();
        for (Region region : all) {
            adcodeById.put(region.getId(), region.getAdcode());
        }
        Map<String, List<Region>> result = new LinkedHashMap<>();
        for (Region region : all) {
            String parentAdcode = region.getParentId() == null ? null : adcodeById.get(region.getParentId());
            if (parentAdcode == null) {
                continue;
            }
            result.computeIfAbsent(parentAdcode, key -> new ArrayList<>()).add(region);
        }
        return result;
    }
}
