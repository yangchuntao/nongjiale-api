package com.nongjiale.repository;

import com.nongjiale.domain.BookingIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingIntentRepository extends JpaRepository<BookingIntent, Long> {
    Optional<BookingIntent> findByIdempotencyKey(String idempotencyKey);
    Page<BookingIntent> findByStatus(String status, Pageable pageable);
    Page<BookingIntent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<BookingIntent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByHomestayId(Long homestayId);

    @Query("""
            SELECT b FROM BookingIntent b
            WHERE b.homestayId IN (SELECT h.id FROM Homestay h WHERE h.merchantId = :merchantId)
              AND (:storeId IS NULL OR b.homestayId = :storeId)
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<BookingIntent> searchForMerchant(@Param("merchantId") Long merchantId,
                                          @Param("storeId") Long storeId,
                                          @Param("status") String status,
                                          Pageable pageable);

    @Query("""
            SELECT COUNT(b) FROM BookingIntent b
            WHERE b.status = 'PENDING'
              AND b.homestayId IN (SELECT h.id FROM Homestay h WHERE h.merchantId = :merchantId)
            """)
    long countPendingForMerchant(@Param("merchantId") Long merchantId);
}
