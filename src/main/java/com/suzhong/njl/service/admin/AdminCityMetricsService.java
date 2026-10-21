package com.suzhong.njl.service.admin;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.MetricsWindow;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.RegionIntentAggregate;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.CityIntentMetricRow;
import com.suzhong.njl.web.admin.dto.CityIntentMetricsResponse;

/**
 * 城市经营指标服务（运营端）。
 *
 * <p>职责：为 {@code GET /api/admin/metrics/city-intents} 与其 CSV 导出提供按市汇总的意向数、
 * 已确认数、已入住数。时间窗由 {@link MetricsWindow} 解析，只有 7 / 30 / ALL 三档；
 * 7 与 30 是从当前时刻往前滚动的天数（见 {@link MetricsWindow#from}），不是自然日零点。
 *
 * <p>事务边界：只读，不开事务；统计在内存里按城市归并，不写数据库聚合。
 *
 * <p>计数口径：
 * <ul>
 *   <li>意向数统计窗口内全部意向，含已改期 {@code RESCHEDULED}；</li>
 *   <li>已确认只数当前状态为 {@code CONFIRMED} 的行，已入住只数 {@code CHECKED_IN}，两者互斥；</li>
 *   <li>归属市由门店地区经 {@link CityRules#owningCityId} 推导，
 *       挂省或无法归属的意向不进任何市，也不进合计。</li>
 * </ul>
 */
@Service
public class AdminCityMetricsService {

    private final RegionMapper regionMapper;
    private final BookingIntentMapper bookingIntentMapper;
    private final Clock applicationClock;

    public AdminCityMetricsService(
            RegionMapper regionMapper,
            BookingIntentMapper bookingIntentMapper,
            Clock applicationClock) {
        this.regionMapper = regionMapper;
        this.bookingIntentMapper = bookingIntentMapper;
        this.applicationClock = applicationClock;
    }

    /**
     * 汇总指定时间窗内的城市意向指标。
     *
     * <p>流程：解析并校验窗口 → 确定统计范围（单个市 / 某省的全部启用市 / 全部启用市）→ 计算时间下界 →
     * SQL 按门店地区聚合窗口内意向（GROUP BY 下推，不再全量拉意向实体）→
     * 把地区聚合行经 {@link CityRules#owningCityId} 归属到市 → 首行放合计、其后放各市明细。
     *
     * @param windowRaw 时间窗参数，取 {@code 7}、{@code 30}、{@code ALL}；null 或空白默认 {@code 7}
     * @param cityId    可选，限定单个市
     * @param province  可选，按省名筛选该省下全部启用市；与 cityId 同时传时以 cityId 为准
     * @return 实际使用的窗口、时间下界（{@code ALL} 为 null）与行列表，第一行恒为合计
     * @throws com.suzhong.njl.web.ApiException 窗口取值非法时 400「参数不正确」；
     *         指定市不存在、不是市或已停用时 404「城市不存在」
     */
    public CityIntentMetricsResponse summary(String windowRaw, Long cityId, String province) {
        MetricsWindow window = parseWindow(windowRaw);
        List<Region> cities = selectedCities(cityId, blankToNull(province));
        OffsetDateTime from = window.from(applicationClock);
        Map<Long, long[]> tally = tally(bookingIntentMapper.aggregateByRegion(from == null ? null : from.toLocalDateTime()), cityIds(cities));
        List<CityIntentMetricRow> rows = new ArrayList<>(cities.size() + 1);
        long intentTotal = 0;
        long confirmedTotal = 0;
        long checkedInTotal = 0;
        List<CityIntentMetricRow> cityRows = new ArrayList<>(cities.size());
        for (Region city : cities) {
            long[] counts = tally.get(city.getId());
            cityRows.add(new CityIntentMetricRow(
                    city.getId(), city.getName(), counts[0], counts[1], counts[2], false));
            intentTotal += counts[0];
            confirmedTotal += counts[1];
            checkedInTotal += counts[2];
        }
        // 契约要求合计固定在第一行，其后才是各市明细；合计由各市行累加，不含无法归属的意向
        rows.add(new CityIntentMetricRow(null, "合计", intentTotal, confirmedTotal, checkedInTotal, true));
        rows.addAll(cityRows);
        return new CityIntentMetricsResponse(window.code(), from, rows);
    }

    /**
     * 导出与 {@link #summary} 同口径的 CSV。
     *
     * <p>列固定为「市,意向数,已确认,已入住」，第一数据行是合计；首部写 UTF-8 BOM，
     * 便于 Excel 直接识别中文。单元格做 CSV 转义与公式注入防护，见 {@link #cell}。
     *
     * @param windowRaw 同 {@link #summary}
     * @param cityId    同 {@link #summary}
     * @param province  同 {@link #summary}
     * @return UTF-8 编码的 CSV 字节，由控制器按 {@code text/csv} 输出
     * @throws com.suzhong.njl.web.ApiException 与 {@link #summary} 相同：窗口非法 400，城市不存在 404
     */
    public byte[] export(String windowRaw, Long cityId, String province) {
        CityIntentMetricsResponse data = summary(windowRaw, cityId, province);
        StringBuilder csv = new StringBuilder();
        // BOM 让 Excel 按 UTF-8 打开，避免中文市名乱码
        csv.append('\uFEFF');
        csv.append("市,意向数,已确认,已入住\n");
        for (CityIntentMetricRow row : data.rows()) {
            csv.append(cell(row.cityName())).append(',');
            csv.append(row.intentCount()).append(',');
            csv.append(row.confirmedCount()).append(',');
            csv.append(row.checkedInCount()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 解析时间窗；非法取值统一转成 400，避免枚举异常冒成 500。 */
    private MetricsWindow parseWindow(String raw) {
        try {
            return MetricsWindow.parse(raw);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * 取统计范围内的市：两者都未指定时取全部启用的市；指定市时校验存在、层级为市且已启用；
     * 指定省名时取该省下全部启用的市（按父省名精确匹配，无匹配市时返回空列表）。
     */
    private List<Region> selectedCities(Long cityId, String province) {
        if (cityId == null && province == null) {
            return enabledCities();
        }
        if (cityId != null) {
            Region city = regionMapper.selectById(cityId);
            // 停用的市与不存在的市同样按 404 处理，避免运营在已下线节点上继续看数
            if (city == null || city.getLevel() != RegionLevel.CITY || !Boolean.TRUE.equals(city.getEnabled())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "城市不存在");
            }
            return List.of(city);
        }
        Map<Long, Region> byId = regionsById();
        List<Region> matched = new ArrayList<>();
        for (Region city : enabledCities()) {
            if (province.equals(provinceName(city, byId))) {
                matched.add(city);
            }
        }
        return matched;
    }

    /** 取全部启用的市，按 {@code sortOrder}、id 升序。 */
    private List<Region> enabledCities() {
        return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .eq(Region::getEnabled, true)
                .eq(Region::getLevel, RegionLevel.CITY)
                .orderByAsc(Region::getSortOrder)
                .orderByAsc(Region::getId));
    }

    /** 一次性加载全部地区，用于补省名。 */
    private Map<Long, Region> regionsById() {
        Map<Long, Region> byId = new HashMap<>();
        for (Region region : regionMapper.selectList(Wrappers.<Region>lambdaQuery())) {
            byId.put(region.getId(), region);
        }
        return byId;
    }

    /** 父节点恰好是省时返回省名，否则 null。 */
    private String provinceName(Region city, Map<Long, Region> byId) {
        if (city.getParentId() == null) {
            return null;
        }
        Region parent = byId.get(city.getParentId());
        if (parent == null || parent.getLevel() != RegionLevel.PROVINCE) {
            return null;
        }
        return parent.getName();
    }

    /** 空白串转 null，便于统一按「未传」处理。 */
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 把按地区聚合的意向行归并到市，得到 [意向数, 已确认, 已入住] 三元组。
     *
     * <p>未匹配到目标市（地区缺失、挂省、归属市不在本次范围）的聚合行直接丢弃。
     */
    private Map<Long, long[]> tally(List<RegionIntentAggregate> aggregates, Set<Long> cityIds) {
        Map<Long, long[]> tally = new HashMap<>();
        // 先为零填充，保证没有意向的市也出现在结果里
        for (Long cityId : cityIds) {
            tally.put(cityId, new long[3]);
        }
        if (aggregates.isEmpty() || cityIds.isEmpty()) {
            return tally;
        }
        Set<Long> regionIds = new HashSet<>();
        for (RegionIntentAggregate aggregate : aggregates) {
            if (aggregate.getRegionId() != null) {
                regionIds.add(aggregate.getRegionId());
            }
        }
        Map<Long, Region> regionById = loadRegions(regionIds);
        for (RegionIntentAggregate aggregate : aggregates) {
            Long owner = CityRules.owningCityId(regionById.get(aggregate.getRegionId()), regionById);
            // 无法归属到本次统计市的意向不计入任何市，也不进合计，避免总数对不上明细
            if (owner == null || !tally.containsKey(owner)) {
                continue;
            }
            long[] counts = tally.get(owner);
            counts[0] += aggregate.getIntentCount();
            // 已改期只计入意向数，不计入已确认；已确认与已入住在 SQL 里按当前状态互斥统计
            counts[1] += aggregate.getConfirmedCount();
            counts[2] += aggregate.getCheckedInCount();
        }
        return tally;
    }

    /** 批量加载地区并补一层父节点，用于把区县门店归到所属市；已加载的 ID 不重复查。 */
    private Map<Long, Region> loadRegions(Set<Long> regionIds) {
        Map<Long, Region> byId = new HashMap<>();
        if (regionIds.isEmpty()) {
            return byId;
        }
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery().in(Region::getId, regionIds));
        Set<Long> parentIds = new HashSet<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
            if (region.getParentId() != null) {
                parentIds.add(region.getParentId());
            }
        }
        parentIds.removeAll(byId.keySet());
        if (!parentIds.isEmpty()) {
            List<Region> parents = regionMapper.selectList(Wrappers.<Region>lambdaQuery().in(Region::getId, parentIds));
            for (Region parent : parents) {
                byId.put(parent.getId(), parent);
            }
        }
        return byId;
    }

    /** 提取市 ID 集合。 */
    private Set<Long> cityIds(List<Region> cities) {
        Set<Long> ids = new HashSet<>();
        for (Region city : cities) {
            ids.add(city.getId());
        }
        return ids;
    }

    /**
     * CSV 单元格转义。
     *
     * <p>以 {@code =}、{@code +}、{@code -}、{@code @} 开头的内容前加单引号，防止被表格软件当公式执行；
     * 含逗号、双引号、换行或回车时整体加双引号，内部双引号翻倍。
     */
    private String cell(String value) {
        String text = value == null ? "" : value;
        if (text.startsWith("=") || text.startsWith("+") || text.startsWith("-") || text.startsWith("@")) {
            text = "'" + text;
        }
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
