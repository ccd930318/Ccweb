package com.ccweb.babytracker.domain.vaccination;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VaccineScheduleRepository extends JpaRepository<VaccineSchedule, Long> {
    List<VaccineSchedule> findAllByOrderByRecommendedAgeMonthsAscDoseNumberAsc();
}
