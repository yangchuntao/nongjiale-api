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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.MetricsWindow;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
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
    private final StoreMapper storeMapper;
    private final BookingIntentMapper bookingIntentMapper;
    private final Clock applicationClock;

    public AdminCityMetricsService(
            RegionMapper regionMapper,
            StoreMapper storeMapper,
            BookingIntentMapper bookingIntentMapper,
            Clock applicationClock) {
        this.regionMapper = regionMapper;
        this.storeMapper = storeMapper;
        this.bookingIntentMapper = bookingIntentMapper;
        this.applicationClock = applicationClock;
    }

    /**
     * 汇总指定时间窗内的城市意向指标。
     *
     * <p>流程：解析并校验窗口 → 确定统计范围（单个市或全部启用市）→ 计算时间下界 →
     * 查窗口内意向 → 批量补门店与地区 → 按归属市归并计数 → 首行放合计、其后放各市明细。
     *
     * @param windowRaw 时间窗参数，取 {@code 7}、{@code 30}、{@code ALL}；null 或空白默认 {@code 7}
     * @param cityId    可选，限定单个市；为 null 时统计全部 {@code enabled=true} 的市
     * @return 实际使用的窗口、时间下界（{@code ALL} 为 null）与行列表，第一行恒为合计
     * @throws com.suzhong.njl.web.ApiException 窗口取值非法时 400「参数不正确」；
     *         指定市不存在、不是市或已停用时 404「城市不存在」
     */
    public CityIntentMetricsResponse summary(String windowRaw, Long cityId) {
        MetricsWindow window = parseWindow(windowRaw);
        List<Region> cities = selectedCities(cityId);
        OffsetDateTime from = window.from(applicationClock);
        Map<Long, long[]> tally = tally(intentsSince(from), cityIds(cities));
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
     * @return UTF-8 编码的 CSV 字节，由控制器按 {@code text/csv} 输出
     * @throws com.suzhong.njl.web.ApiException 与 {@link #summary} 相同：窗口非法 400，城市不存在 404
     */
    public byte[] export(String windowRaw, Long cityId) {
        CityIntentMetricsResponse data = summary(windowRaw, cityId);
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

    /** 取统计范围内的市：未指定时取全部启用的市，指定时校验存在、层级为市且已启用。 */
    private List<Region> selectedCities(Long cityId) {
        if (cityId == null) {
            return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getEnabled, true)
                    .eq(Region::getLevel, RegionLevel.CITY)
                    .orderByAsc(Region::getSortOrder)
                    .orderByAsc(Region::getId));
        }
        Region city = regionMapper.selectById(cityId);
        // 停用的市与不存在的市同样按 404 处理，避免运营在已下线节点上继续看数
        if (city == null || city.getLevel() != RegionLevel.CITY || !Boolean.TRUE.equals(city.getEnabled())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "城市不存在");
        }
        return List.of(city);
    }

    /** 查窗口内的意向；{@code ALL} 时 {@code from} 为 null，不加时间条件。 */
    private List<BookingIntent> intentsSince(OffsetDateTime from) {
        LambdaQueryWrapper<BookingIntent> query = Wrappers.lambdaQuery();
        if (from != null) {
            query.ge(BookingIntent::getCreatedAt, from.toLocalDateTime());
        }
        return bookingIntentMapper.selectList(query);
    }

    /**
     * 把意向按归属市归并成 [意向数, 已确认, 已入住] 三元组。
     *
     * <p>未匹配到目标市（门店缺失、地区挂省、归属市不在本次范围）的记录直接丢弃。
     */
    private Map<Long, long[]> tally(List<BookingIntent> intents, Set<Long> cityIds) {
        Map<Long, long[]> tally = new HashMap<>();
        // 先为零填充，保证没有意向的市也出现在结果里
        for (Long cityId : cityIds) {
            tally.put(cityId, new long[3]);
        }
        if (intents.isEmpty() || cityIds.isEmpty()) {
            return tally;
        }
        Set<Long> storeIds = new HashSet<>();
        for (BookingIntent intent : intents) {
            if (intent.getStoreId() != null) {
                storeIds.add(intent.getStoreId());
            }
        }
        if (storeIds.isEmpty()) {
            return tally;
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery().in(Store::getId, storeIds));
        Map<Long, Store> storeById = new HashMap<>();
        Set<Long> regionIds = new HashSet<>();
        for (Store store : stores) {
            storeById.put(store.getId(), store);
            if (store.getRegionId() != null) {
                regionIds.add(store.getRegionId());
            }
        }
        Map<Long, Region> regionById = loadRegions(regionIds);
        for (BookingIntent intent : intents) {
            Store store = storeById.get(intent.getStoreId());
            if (store == null) {
                continue;
            }
            Long owner = CityRules.owningCityId(regionById.get(store.getRegionId()), regionById);
            // 无法归属到本次统计市的意向不计入任何市，也不进合计，避免总数对不上明细
            if (owner == null || !tally.containsKey(owner)) {
                continue;
            }
            long[] counts = tally.get(owner);
            counts[0]++;
            // 已改期只计入意向数，不计入已确认；已确认与已入住按当前状态互斥统计
            if (intent.getStatus() == IntentStatus.CONFIRMED) {
                counts[1]++;
            } else if (intent.getStatus() == IntentStatus.CHECKED_IN) {
                counts[2]++;
            }
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
