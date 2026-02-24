package com.ccweb.babytracker.domain.vaccination;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {
    @EntityGraph(attributePaths = "schedule")
    List<VaccinationRecord> findByBabyIdOrderByAdministeredDateDesc(UUID babyId);
}
