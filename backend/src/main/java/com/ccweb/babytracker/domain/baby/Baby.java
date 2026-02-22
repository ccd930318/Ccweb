package com.ccweb.babytracker.domain.baby;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "babies")
@Getter @Setter @NoArgsConstructor
public class Baby {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 1)
    private String gender;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
