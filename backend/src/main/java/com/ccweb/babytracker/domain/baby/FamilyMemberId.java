package com.ccweb.babytracker.domain.baby;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode
public class FamilyMemberId implements Serializable {
    private UUID babyId;
    private UUID userId;
}
