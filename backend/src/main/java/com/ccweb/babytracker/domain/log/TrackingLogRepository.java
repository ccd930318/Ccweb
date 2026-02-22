package com.ccweb.babytracker.domain.log;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TrackingLogRepository extends JpaRepository<TrackingLog, UUID> {
    List<TrackingLog> findByBabyIdOrderByOccurredAtDesc(UUID babyId);
}
