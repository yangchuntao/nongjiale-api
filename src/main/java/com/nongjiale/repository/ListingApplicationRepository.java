package com.nongjiale.repository;

import com.nongjiale.domain.ListingApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListingApplicationRepository extends JpaRepository<ListingApplication, Long> {
    boolean existsByHomestayIdAndActionAndStatus(Long homestayId, String action, String status);
    boolean existsByHomestayIdAndStatus(Long homestayId, String status);
    Optional<ListingApplication> findFirstByHomestayIdAndStatus(Long homestayId, String status);
    List<ListingApplication> findByHomestayId(Long homestayId);
    long countByMerchantIdAndStatus(Long merchantId, String status);
    long countByStatus(String status);

    @Query("""
            SELECT a FROM ListingApplication a
            WHERE a.merchantId = :merchantId
              AND (:storeId IS NULL OR a.homestayId = :storeId)
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<ListingApplication> searchMine(@Param("merchantId") Long merchantId,
                                        @Param("storeId") Long storeId,
                                        @Param("status") String status,
                                        Pageable pageable);

    @Query("""
            SELECT a FROM ListingApplication a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:action IS NULL OR a.action = :action)
            """)
    Page<ListingApplication> searchAdmin(@Param("status") String status,
                                         @Param("action") String action,
                                         Pageable pageable);
}
