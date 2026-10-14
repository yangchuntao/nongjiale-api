package com.nongjiale.service;

import com.nongjiale.domain.Homestay;
import com.nongjiale.domain.HomestayRoom;
import com.nongjiale.domain.HomestayTag;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.HomestayRoomRepository;
import com.nongjiale.repository.HomestayTagRepository;
import com.nongjiale.repository.ListingApplicationRepository;
import com.nongjiale.repository.MerchantRepository;
import com.nongjiale.repository.RegionRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 商家门店读写。storeId == homestay.id；套餐 package 落在 homestay_room。
 * 请求体里的 merchantId / status / salesCount / recommendWeight 不绑定，保存时不生效。
 */
@Service
public class MerchantStoreService {
    private final HomestayRepository homestays;
    private final HomestayRoomRepository rooms;
    private final HomestayTagRepository tags;
    private final ListingApplicationRepository listings;
    private final BookingIntentRepository intents;
    private final RegionRepository regions;
    private final MerchantRepository merchants;
    private final JsonStrings json;

    public MerchantStoreService(HomestayRepository homestays, HomestayRoomRepository rooms,
                                HomestayTagRepository tags, ListingApplicationRepository listings,
                                BookingIntentRepository intents, RegionRepository regions,
                                MerchantRepository merchants, JsonStrings json) {
        this.homestays = homestays;
        this.rooms = rooms;
        this.tags = tags;
        this.listings = listings;
        this.intents = intents;
        this.regions = regions;
        this.merchants = merchants;
        this.json = json;
    }

    public Page<Map<String, Object>> list(Long merchantId, int page, int pageSize) {
        return homestays.findByMerchantId(merchantId, PageRequests.of(page, pageSize)).map(this::toDto);
    }

    public Map<String, Object> get(Long merchantId, Long storeId) {
        return toDto(own(merchantId, storeId));
    }

    @Transactional
    public Map<String, Object> create(Long merchantId, String name, Long regionId, String address, String phone,
                                      String summary, String coverUrl, List<String> gallery, String facilities,
                                      String notice, String traffic, String surroundings) {
        requireRegion(regionId);
        if (name == null || name.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "名称不能为空");
        }
        Homestay h = new Homestay();
        h.setMerchantId(merchantId);
        h.setName(name.trim());
        h.setRegionId(regionId);
        h.setAddress(address == null ? "" : address);
        h.setPhone(phone);
        h.setSummary(summary);
        h.setCoverUrl(coverUrl);
        h.setGallery(json.write(gallery == null ? List.of() : gallery));
        h.setFacilities(facilities);
        h.setNotice(notice);
        h.setTraffic(traffic);
        h.setSurroundings(surroundings);
        h.setStatus("DRAFT");
        h.setSalesCount(0);
        return toDto(homestays.saveAndFlush(h));
    }

    @Transactional
    public Map<String, Object> update(Long merchantId, Long storeId, Long version, String name, Long regionId,
                                      String address, String phone, String summary, String coverUrl,
                                      List<String> gallery, String facilities, String notice, String traffic,
                                      String surroundings) {
        Homestay h = own(merchantId, storeId);
        if (version != null && !version.equals(h.getVersion())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "数据已变更，请刷新后重试");
        }
        if (name != null) {
            if (name.isBlank()) throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "名称不能为空");
            h.setName(name.trim());
        }
        if (regionId != null) {
            requireRegion(regionId);
            h.setRegionId(regionId);
        }
        if (address != null) h.setAddress(address);
        if (phone != null) h.setPhone(phone);
        if (summary != null) h.setSummary(summary);
        if (coverUrl != null) h.setCoverUrl(coverUrl);
        if (gallery != null) h.setGallery(json.write(gallery));
        if (facilities != null) h.setFacilities(facilities);
        if (notice != null) h.setNotice(notice);
        if (traffic != null) h.setTraffic(traffic);
        if (surroundings != null) h.setSurroundings(surroundings);
        return toDto(homestays.saveAndFlush(h));
    }

    @Transactional
    public void delete(Long merchantId, Long storeId) {
        Homestay h = own(merchantId, storeId);
        if (!"DRAFT".equals(h.getStatus()) && !"OFF_SALE".equals(h.getStatus())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, 422, "仅草稿或已下架门店可删除");
        }
        if (listings.existsByHomestayIdAndStatus(storeId, "PENDING")) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, 422, "存在待审核的上下架申请");
        }
        if (intents.existsByHomestayId(storeId) || homestays.countReviews(storeId) > 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, 422, "门店存在关联数据，不能删除");
        }
        tags.deleteAll(tags.findByHomestayIdOrderByIdAsc(storeId));
        rooms.deleteAll(rooms.findByHomestayIdOrderBySortOrderAsc(storeId));
        listings.deleteAll(listings.findByHomestayId(storeId));
        homestays.delete(h);
    }

    public List<Map<String, Object>> packages(Long merchantId, Long storeId) {
        own(merchantId, storeId);
        return rooms.findByHomestayIdOrderBySortOrderAsc(storeId).stream().map(this::packageDto).toList();
    }

    @Transactional
    public Map<String, Object> createPackage(Long merchantId, Long storeId, String name, String description,
                                             BigDecimal priceFrom, Integer sortOrder) {
        Homestay h = own(merchantId, storeId);
        HomestayRoom room = new HomestayRoom();
        room.setHomestayId(storeId);
        applyPackage(room, name, description, priceFrom, sortOrder == null ? 0 : sortOrder);
        HomestayRoom saved = rooms.save(room);
        refreshPriceFrom(h);
        return packageDto(saved);
    }

    @Transactional
    public Map<String, Object> updatePackage(Long merchantId, Long storeId, Long packageId, String name,
                                             String description, BigDecimal priceFrom, Integer sortOrder) {
        Homestay h = own(merchantId, storeId);
        HomestayRoom room = rooms.findByIdAndHomestayId(packageId, storeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "套餐不存在"));
        if (name != null) {
            if (name.isBlank()) throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "套餐名称不能为空");
            room.setName(name.trim());
        }
        if (description != null) room.setDescription(description);
        if (priceFrom != null) {
            if (priceFrom.signum() < 0) throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "价格不能为负");
            room.setPriceFrom(priceFrom);
        }
        if (sortOrder != null) room.setSortOrder(sortOrder);
        refreshPriceFrom(h);
        return packageDto(room);
    }

    @Transactional
    public void deletePackage(Long merchantId, Long storeId, Long packageId) {
        Homestay h = own(merchantId, storeId);
        HomestayRoom room = rooms.findByIdAndHomestayId(packageId, storeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "套餐不存在"));
        rooms.delete(room);
        rooms.flush();
        refreshPriceFrom(h);
    }

    public Map<String, Object> tags(Long merchantId, Long storeId) {
        own(merchantId, storeId);
        return Map.of("tags", tagNames(storeId));
    }

    @Transactional
    public Map<String, Object> replaceTags(Long merchantId, Long storeId, List<String> incoming) {
        own(merchantId, storeId);
        tags.deleteAll(tags.findByHomestayIdOrderByIdAsc(storeId));
        tags.flush();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (incoming != null) {
            for (String raw : incoming) {
                if (raw == null || raw.isBlank()) continue;
                unique.add(raw.trim());
            }
        }
        List<HomestayTag> saved = new ArrayList<>();
        for (String name : unique) {
            HomestayTag tag = new HomestayTag();
            tag.setHomestayId(storeId);
            tag.setTag(name);
            saved.add(tag);
        }
        tags.saveAll(saved);
        return Map.of("tags", saved.stream().map(HomestayTag::getTag).toList());
    }

    @Transactional
    public Map<String, Object> assignOwner(Long homestayId, Long merchantId) {
        Homestay h = homestays.findById(homestayId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "房源不存在"));
        if (merchantId == null || merchants.findById(merchantId).isEmpty()) {
            throw new BusinessException(merchantId == null ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND,
                    merchantId == null ? 400 : 404,
                    merchantId == null ? "merchantId 不能为空" : "商家不存在");
        }
        h.setMerchantId(merchantId);
        homestays.save(h);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", h.getId());
        body.put("merchantId", h.getMerchantId());
        body.put("name", h.getName());
        return body;
    }

    public String listingStatus(Long homestayId) {
        return listings.findFirstByHomestayIdAndStatus(homestayId, "PENDING")
                .map(a -> "PUBLISH".equals(a.getAction()) ? "PUBLISH_PENDING" : "UNPUBLISH_PENDING")
                .orElse("NONE");
    }

    public Map<String, Object> toDto(Homestay h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("merchantId", h.getMerchantId());
        m.put("name", h.getName());
        m.put("regionId", h.getRegionId());
        m.put("address", h.getAddress());
        m.put("phone", h.getPhone());
        m.put("summary", h.getSummary());
        m.put("coverUrl", h.getCoverUrl());
        m.put("gallery", json.read(h.getGallery()));
        m.put("facilities", h.getFacilities());
        m.put("notice", h.getNotice());
        m.put("traffic", h.getTraffic());
        m.put("surroundings", h.getSurroundings());
        m.put("status", h.getStatus());
        m.put("listingStatus", listingStatus(h.getId()));
        m.put("priceFrom", h.getPriceFrom());
        m.put("salesCount", h.getSalesCount());
        m.put("version", h.getVersion());
        return m;
    }

    private Homestay own(Long merchantId, Long storeId) {
        return homestays.findByIdAndMerchantId(storeId, merchantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "门店不存在"));
    }

    private void requireRegion(Long regionId) {
        if (regionId == null || regions.findById(regionId).isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "地区不存在");
        }
    }

    private void applyPackage(HomestayRoom room, String name, String description, BigDecimal priceFrom, int sortOrder) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "套餐名称不能为空");
        }
        if (priceFrom == null || priceFrom.signum() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "价格不能为负");
        }
        room.setName(name.trim());
        room.setDescription(description);
        room.setPriceFrom(priceFrom);
        room.setSortOrder(sortOrder);
    }

    private void refreshPriceFrom(Homestay homestay) {
        BigDecimal min = rooms.findByHomestayIdOrderBySortOrderAsc(homestay.getId()).stream()
                .map(HomestayRoom::getPriceFrom)
                .filter(p -> p != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        homestay.setPriceFrom(min);
        homestays.save(homestay);
    }

    private List<String> tagNames(Long storeId) {
        return tags.findByHomestayIdOrderByIdAsc(storeId).stream().map(HomestayTag::getTag).toList();
    }

    private Map<String, Object> packageDto(HomestayRoom room) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", room.getId());
        m.put("storeId", room.getHomestayId());
        m.put("name", room.getName());
        m.put("description", room.getDescription() == null ? "" : room.getDescription());
        m.put("priceFrom", room.getPriceFrom());
        m.put("sortOrder", room.getSortOrder());
        return m;
    }
}
