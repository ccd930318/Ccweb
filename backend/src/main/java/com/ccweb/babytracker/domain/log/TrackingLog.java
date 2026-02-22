package com.ccweb.babytracker.domain.log;

import com.ccweb.babytracker.domain.baby.Baby;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_logs")
@Getter @Setter @NoArgsConstructor
public class TrackingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Instant occurredAt;

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
