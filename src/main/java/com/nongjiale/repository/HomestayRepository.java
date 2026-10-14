package com.nongjiale.repository;

import com.nongjiale.domain.Homestay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface HomestayRepository extends JpaRepository<Homestay, Long> {

    @Query("""
        SELECT h FROM Homestay h
        WHERE h.status = 'ON_SALE'
          AND (:regionId IS NULL OR h.regionId = :regionId)
          AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:priceMin IS NULL OR h.priceFrom >= :priceMin)
          AND (:priceMax IS NULL OR h.priceFrom <= :priceMax)
        """)
    Page<Homestay> search(@Param("regionId") Long regionId,
                          @Param("keyword") String keyword,
                          @Param("priceMin") BigDecimal priceMin,
                          @Param("priceMax") BigDecimal priceMax,
                          Pageable pageable);
}
