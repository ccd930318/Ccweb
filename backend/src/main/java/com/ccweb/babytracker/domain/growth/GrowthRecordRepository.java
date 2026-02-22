package com.ccweb.babytracker.domain.growth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GrowthRecordRepository extends JpaRepository<GrowthRecord, UUID> {
    List<GrowthRecord> findByBabyIdOrderByMeasurementDateDesc(UUID babyId);
}
