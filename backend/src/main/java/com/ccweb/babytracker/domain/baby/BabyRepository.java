package com.ccweb.babytracker.domain.baby;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BabyRepository extends JpaRepository<Baby, UUID> {

    @Query("SELECT fm.baby FROM FamilyMember fm WHERE fm.user.id = :userId")
    List<Baby> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT fm.baby FROM FamilyMember fm WHERE fm.baby.id = :babyId AND fm.user.id = :userId")
    Optional<Baby> findByIdAndUserId(@Param("babyId") UUID babyId, @Param("userId") UUID userId);
}
