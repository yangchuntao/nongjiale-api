package com.nongjiale.repository;

import com.nongjiale.domain.IntentNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntentNoteRepository extends JpaRepository<IntentNote, Long> {
    List<IntentNote> findByIntentIdOrderByCreatedAtAsc(Long intentId);
    Optional<IntentNote> findFirstByIntentIdOrderByCreatedAtDesc(Long intentId);
}
