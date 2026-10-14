package com.nongjiale.repository;

import com.nongjiale.domain.BookingIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookingIntentRepository extends JpaRepository<BookingIntent, Long> {
    Optional<BookingIntent> findByIdempotencyKey(String idempotencyKey);
    Page<BookingIntent> findByStatus(String status, Pageable pageable);
    Page<BookingIntent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
