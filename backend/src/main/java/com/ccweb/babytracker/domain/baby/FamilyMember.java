package com.ccweb.babytracker.domain.baby;

import com.ccweb.babytracker.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "family_members")
@Getter @Setter @NoArgsConstructor
public class FamilyMember {

    @EmbeddedId
    private FamilyMemberId id = new FamilyMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("babyId")
    @JoinColumn(name = "baby_id")
    private Baby baby;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String role = "viewer";
}
