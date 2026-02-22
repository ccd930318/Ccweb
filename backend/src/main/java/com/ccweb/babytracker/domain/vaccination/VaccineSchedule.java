package com.ccweb.babytracker.domain.vaccination;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vaccine_schedules")
@Getter @Setter @NoArgsConstructor
public class VaccineSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vaccineName;

    @Column(nullable = false)
    private int doseNumber;

    @Column(nullable = false)
    private int recommendedAgeMonths;

    private String notes;
}
