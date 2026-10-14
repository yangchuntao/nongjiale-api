package com.nongjiale.repository;

import com.nongjiale.domain.MerchantApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, Long> {
    Page<MerchantApplication> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<MerchantApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);

    @Query("""
            SELECT COUNT(a)
            FROM MerchantApplication a
            WHERE a.status = :status
              AND (a.phone = :phone OR (:userId IS NOT NULL AND a.userId = :userId))
            """)
    long countByStatusForApplicant(@Param("status") String status,
                                   @Param("phone") String phone,
                                   @Param("userId") Long userId);

    @Query("""
            SELECT a FROM MerchantApplication a
            WHERE (:userId IS NOT NULL AND a.userId = :userId) OR a.phone = :phone
            ORDER BY a.createdAt DESC
            """)
    List<MerchantApplication> findMine(@Param("userId") Long userId, @Param("phone") String phone);
}
