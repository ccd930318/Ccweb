package com.ccweb.babytracker.domain.food;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findAllByOrderByRecommendedAgeMonthsAscNameAsc();
}
