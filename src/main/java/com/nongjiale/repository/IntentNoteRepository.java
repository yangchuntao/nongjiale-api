package com.nongjiale.repository;

import com.nongjiale.domain.IntentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntentNoteRepository extends JpaRepository<IntentNote, Long> {
    List<IntentNote> findByIntentIdOrderByCreatedAtAsc(Long intentId);
}
