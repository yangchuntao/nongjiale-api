package com.nongjiale.repository;

import com.nongjiale.domain.HomestayTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomestayTagRepository extends JpaRepository<HomestayTag, Long> {
    List<HomestayTag> findByHomestayIdOrderByIdAsc(Long homestayId);
}
