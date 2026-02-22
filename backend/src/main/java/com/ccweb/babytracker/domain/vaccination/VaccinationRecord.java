package com.ccweb.babytracker.domain.vaccination;

import com.ccweb.babytracker.domain.baby.Baby;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vaccination_records")
@Getter @Setter @NoArgsConstructor
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private VaccineSchedule schedule;

    @Column(nullable = false)
    private LocalDate administeredDate;

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
