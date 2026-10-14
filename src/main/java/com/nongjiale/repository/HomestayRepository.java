package com.nongjiale.repository;

import com.nongjiale.domain.Homestay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface HomestayRepository extends JpaRepository<Homestay, Long> {
    Page<Homestay> findByMerchantId(Long merchantId, Pageable pageable);
    List<Homestay> findByStatusAndHomeRecommendedTrueOrderByHomeRecommendSortAscIdAsc(String status);
    long countByMerchantId(Long merchantId);
    Optional<Homestay> findByIdAndMerchantId(Long id, Long merchantId);

    @Query(value = "SELECT COUNT(*) FROM review WHERE homestay_id = :id", nativeQuery = true)
    long countReviews(@Param("id") Long id);

    @Query("""
        SELECT h FROM Homestay h
        WHERE h.status = 'ON_SALE'
          AND (:regionId IS NULL OR h.regionId = :regionId)
          AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:priceMin IS NULL OR h.priceFrom >= :priceMin)
          AND (:priceMax IS NULL OR h.priceFrom <= :priceMax)
          AND (:tag IS NULL OR EXISTS (
                SELECT 1 FROM HomestayTag t WHERE t.homestayId = h.id AND t.tag = :tag))
        """)
    Page<Homestay> search(@Param("regionId") Long regionId,
                          @Param("keyword") String keyword,
                          @Param("priceMin") BigDecimal priceMin,
                          @Param("priceMax") BigDecimal priceMax,
                          @Param("tag") String tag,
                          Pageable pageable);
}
