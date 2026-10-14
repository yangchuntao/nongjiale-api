package com.nongjiale.web;

import com.nongjiale.domain.Homestay;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.service.MerchantStoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/homestays")
public class AdminHomestayController {
    private final HomestayRepository homestays;
    private final MerchantStoreService stores;

    public AdminHomestayController(HomestayRepository homestays, MerchantStoreService stores) {
        this.homestays = homestays;
        this.stores = stores;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Homestay> result = homestays.findAll(PageRequest.of(Math.max(page, 1) - 1, Math.min(pageSize, 50)));
        return ApiResponse.ok(new PageResult<>(result.map(this::dto).getContent(), result.getTotalElements()));
    }

    public record StatusReq(String status, Long version) {}
    public record OwnerReq(Long merchantId) {}
    /** 首页推荐字段。没有独立推荐位资源；客服调用本接口 403。 */
    public record RecommendReq(Boolean homeRecommended, Integer homeRecommendSort, Long version) {}

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional
    public ApiResponse<Map<String, Object>> updateRecommend(@PathVariable Long id,
                                                            @RequestBody RecommendReq req,
                                                            Authentication auth) {
        requireOperator(auth);
        if (req == null || (req.homeRecommended() == null && req.homeRecommendSort() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "请提供 homeRecommended 或 homeRecommendSort");
        }
        Homestay h = homestays.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "房源不存在"));
        if (req.version() != null && !req.version().equals(h.getVersion())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "房源已被他人更新，请刷新后重试");
        }
        if (req.homeRecommended() != null) h.setHomeRecommended(req.homeRecommended());
        if (req.homeRecommendSort() != null) h.setHomeRecommendSort(req.homeRecommendSort());
        return ApiResponse.ok(dto(h));
    }

    @PostMapping("/{id}/owner")
    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional
    public ApiResponse<Map<String, Object>> assignOwner(@PathVariable Long id,
                                                        @RequestBody OwnerReq req,
                                                        Authentication auth) {
        requireOperator(auth);
        return ApiResponse.ok(stores.assignOwner(id, req == null ? null : req.merchantId()));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                         @RequestBody StatusReq req,
                                                         Authentication auth) {
        requireOperator(auth);
        Homestay h = homestays.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "房源不存在"));
        if (req.version() != null && !req.version().equals(h.getVersion())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "房源已被他人更新，请刷新后重试");
        }
        if (!"ON_SALE".equals(req.status()) && !"OFF_SALE".equals(req.status()) && !"DRAFT".equals(req.status())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "非法状态");
        }
        h.setStatus(req.status());
        return ApiResponse.ok(dto(h));
    }

    private Map<String, Object> dto(Homestay h) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", h.getId());
        m.put("name", h.getName());
        m.put("status", h.getStatus());
        m.put("priceFrom", h.getPriceFrom());
        m.put("regionId", h.getRegionId());
        m.put("homeRecommended", h.isHomeRecommended());
        m.put("homeRecommendSort", h.getHomeRecommendSort());
        m.put("version", h.getVersion());
        return m;
    }

    private static void requireOperator(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isOperator()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权限");
        }
    }
}
