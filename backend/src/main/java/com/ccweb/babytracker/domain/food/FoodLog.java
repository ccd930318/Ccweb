package com.ccweb.babytracker.domain.food;

import com.ccweb.babytracker.domain.baby.Baby;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "food_logs")
@Getter @Setter @NoArgsConstructor
public class FoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(nullable = false)
    private LocalDate introducedDate;

    private String reaction;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
