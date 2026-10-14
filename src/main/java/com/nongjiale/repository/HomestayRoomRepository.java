package com.nongjiale.repository;

import com.nongjiale.domain.HomestayRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HomestayRoomRepository extends JpaRepository<HomestayRoom, Long> {
    List<HomestayRoom> findByHomestayIdOrderBySortOrderAsc(Long homestayId);
}
