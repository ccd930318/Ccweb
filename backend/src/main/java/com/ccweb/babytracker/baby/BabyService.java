package com.ccweb.babytracker.baby;

import com.ccweb.babytracker.baby.dto.BabyRequest;
import com.ccweb.babytracker.baby.dto.BabyResponse;
import com.ccweb.babytracker.domain.baby.*;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BabyService {

    private final BabyRepository babyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public BabyService(BabyRepository babyRepository,
                       FamilyMemberRepository familyMemberRepository,
                       UserRepository userRepository) {
        this.babyRepository = babyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    public BabyResponse create(BabyRequest req, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Baby baby = new Baby();
        baby.setName(req.name());
        baby.setBirthDate(req.birthDate());
        baby.setGender(req.gender());
        babyRepository.save(baby);

        FamilyMember fm = new FamilyMember();
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(baby.getId());
        fmId.setUserId(userId);
        fm.setId(fmId);
        fm.setBaby(baby);
        fm.setUser(user);
        fm.setRole("owner");
        familyMemberRepository.save(fm);

        return toResponse(baby);
    }

    @Transactional(readOnly = true)
    public List<BabyResponse> listForUser(UUID userId) {
        return babyRepository.findAllByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BabyResponse getById(UUID babyId, UUID userId) {
        return babyRepository.findByIdAndUserId(babyId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private BabyResponse toResponse(Baby b) {
        return new BabyResponse(b.getId(), b.getName(), b.getBirthDate(), b.getGender());
    }
}
