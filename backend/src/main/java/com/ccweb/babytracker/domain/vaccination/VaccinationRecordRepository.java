package com.ccweb.babytracker.domain.vaccination;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {
    List<VaccinationRecord> findByBabyIdOrderByAdministeredDateDesc(UUID babyId);
}
