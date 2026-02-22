package com.ccweb.babytracker.domain.baby;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, FamilyMemberId> {
}
