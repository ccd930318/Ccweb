package com.ccweb.babytracker.domain.invite;

import com.ccweb.babytracker.domain.baby.Baby;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
@Getter @Setter @NoArgsConstructor
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @Column(nullable = false)
    private String inviteeEmail;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private boolean accepted = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;
}
