package com.ccweb.babytracker.domain.food;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FoodLogRepository extends JpaRepository<FoodLog, UUID> {
    @EntityGraph(attributePaths = "foodItem")
    List<FoodLog> findByBabyIdOrderByIntroducedDateDesc(UUID babyId);
}
