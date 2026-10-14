package com.nongjiale.web;

import com.nongjiale.domain.Homestay;
import com.nongjiale.domain.HomestayRoom;
import com.nongjiale.domain.Region;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.HomestayRoomRepository;
import com.nongjiale.repository.RegionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PortalCatalogController {
    private final RegionRepository regions;
    private final HomestayRepository homestays;
    private final HomestayRoomRepository rooms;

    public PortalCatalogController(RegionRepository regions, HomestayRepository homestays, HomestayRoomRepository rooms) {
        this.regions = regions;
        this.homestays = homestays;
        this.rooms = rooms;
    }

    @GetMapping("/regions")
    public ApiResponse<List<Map<String, Object>>> listRegions() {
        List<Map<String, Object>> list = regions.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(r -> Map.<String, Object>of("id", r.getId(), "slug", r.getSlug(), "name", r.getName()))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/home/feed")
    public ApiResponse<Map<String, Object>> homeFeed() {
        Page<Homestay> hot = homestays.search(null, null, null, null,
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "salesCount")));
        List<Map<String, Object>> hotCards = hot.getContent().stream().map(this::card).toList();
        return ApiResponse.ok(Map.of(
                "regions", regions.findByEnabledTrueOrderBySortOrderAsc().stream()
                        .map(r -> Map.<String, Object>of("id", r.getId(), "slug", r.getSlug(), "name", r.getName()))
                        .toList(),
                "hotHomestays", hotCards,
                "topics", List.of(),
                "articles", List.of()
        ));
    }

    @GetMapping("/homestays")
    public ApiResponse<PageResult<Map<String, Object>>> listHomestays(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String regionSlug,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long rid = regionId;
        if (rid == null && regionSlug != null && !regionSlug.isBlank()) {
            rid = regions.findBySlugAndEnabledTrue(regionSlug).map(Region::getId).orElse(-1L);
        }
        int p = Math.max(page, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 50);
        Page<Homestay> result = homestays.search(rid, blankToNull(keyword), priceMin, priceMax,
                PageRequest.of(p, size, Sort.by(Sort.Direction.DESC, "salesCount")));
        return ApiResponse.ok(new PageResult<>(result.map(this::card).getContent(), result.getTotalElements()));
    }

    @GetMapping("/homestays/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Homestay h = homestays.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "农家乐不存在"));
        if (!h.isOnSale()) {
            throw new BusinessException(HttpStatus.GONE, 410, "该农家乐暂不可预订");
        }
        List<Map<String, Object>> roomList = rooms.findByHomestayIdOrderBySortOrderAsc(id).stream()
                .map(this::roomDto).toList();
        Map<String, Object> body = new HashMap<>(card(h));
        body.put("facilities", h.getFacilities());
        body.put("notice", h.getNotice());
        body.put("rooms", roomList);
        body.put("reviews", List.of());
        body.put("nearby", List.of());
        body.put("bookable", true);
        return ApiResponse.ok(body);
    }

    private Map<String, Object> card(Homestay h) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", h.getId());
        m.put("name", h.getName());
        m.put("address", h.getAddress());
        m.put("coverUrl", h.getCoverUrl());
        m.put("salesCount", h.getSalesCount());
        m.put("priceFrom", h.getPriceFrom());
        m.put("status", h.getStatus());
        m.put("regionId", h.getRegionId());
        return m;
    }

    private Map<String, Object> roomDto(HomestayRoom r) {
        return Map.of(
                "id", r.getId(),
                "name", r.getName(),
                "roomCount", r.getRoomCount() == null ? 0 : r.getRoomCount(),
                "priceFrom", r.getPriceFrom()
        );
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
