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
        rows.add(new CityIntentMetricRow(null, "合计", intentTotal, confirmedTotal, checkedInTotal, true));
        rows.addAll(cityRows);
        return new CityIntentMetricsResponse(window.code(), from, rows);
    }

    public byte[] export(String windowRaw, Long cityId) {
        CityIntentMetricsResponse data = summary(windowRaw, cityId);
        StringBuilder csv = new StringBuilder();
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

    private MetricsWindow parseWindow(String raw) {
        try {
            return MetricsWindow.parse(raw);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private List<Region> selectedCities(Long cityId) {
        if (cityId == null) {
            return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                    .eq(Region::getEnabled, true)
                    .eq(Region::getLevel, RegionLevel.CITY)
                    .orderByAsc(Region::getSortOrder)
                    .orderByAsc(Region::getId));
        }
        Region city = regionMapper.selectById(cityId);
        if (city == null || city.getLevel() != RegionLevel.CITY || !Boolean.TRUE.equals(city.getEnabled())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "城市不存在");
        }
        return List.of(city);
    }

    private List<BookingIntent> intentsSince(OffsetDateTime from) {
        LambdaQueryWrapper<BookingIntent> query = Wrappers.lambdaQuery();
        if (from != null) {
            query.ge(BookingIntent::getCreatedAt, from.toLocalDateTime());
        }
        return bookingIntentMapper.selectList(query);
    }

    private Map<Long, long[]> tally(List<BookingIntent> intents, Set<Long> cityIds) {
        Map<Long, long[]> tally = new HashMap<>();
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
            if (owner == null || !tally.containsKey(owner)) {
                continue;
            }
            long[] counts = tally.get(owner);
            counts[0]++;
            if (intent.getStatus() == IntentStatus.CONFIRMED) {
                counts[1]++;
            } else if (intent.getStatus() == IntentStatus.CHECKED_IN) {
                counts[2]++;
            }
        }
        return tally;
    }

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

    private Set<Long> cityIds(List<Region> cities) {
        Set<Long> ids = new HashSet<>();
        for (Region city : cities) {
            ids.add(city.getId());
        }
        return ids;
    }

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
