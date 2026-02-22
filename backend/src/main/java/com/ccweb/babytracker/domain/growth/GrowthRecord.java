package com.ccweb.babytracker.domain.growth;

import com.ccweb.babytracker.domain.baby.Baby;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "growth_records")
@Getter @Setter @NoArgsConstructor
public class GrowthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @Column(nullable = false)
    private LocalDate measurementDate;

    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal headCm;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
