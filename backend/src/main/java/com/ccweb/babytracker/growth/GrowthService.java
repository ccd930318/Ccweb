package com.ccweb.babytracker.growth;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.growth.GrowthRecord;
import com.ccweb.babytracker.domain.growth.GrowthRecordRepository;
import com.ccweb.babytracker.growth.dto.GrowthRequest;
import com.ccweb.babytracker.growth.dto.GrowthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GrowthService {

    private final GrowthRecordRepository growthRepo;
    private final BabyRepository babyRepo;
    private final FamilyMemberRepository familyMemberRepo;
    private final WhoPercentileService who;

    public GrowthService(GrowthRecordRepository growthRepo,
                         BabyRepository babyRepo,
                         FamilyMemberRepository familyMemberRepo,
                         WhoPercentileService who) {
        this.growthRepo = growthRepo;
        this.babyRepo = babyRepo;
        this.familyMemberRepo = familyMemberRepo;
        this.who = who;
    }

    private void checkMembership(UUID babyId, UUID userId) {
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(babyId);
        fmId.setUserId(userId);
        if (!familyMemberRepo.existsById(fmId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public GrowthResponse create(UUID babyId, UUID userId, GrowthRequest req) {
        checkMembership(babyId, userId);
        Baby baby = babyRepo.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        GrowthRecord rec = new GrowthRecord();
        rec.setBaby(baby);
        rec.setMeasurementDate(req.measurementDate());
        rec.setWeightKg(req.weightKg());
        rec.setHeightCm(req.heightCm());
        rec.setHeadCm(req.headCm());
        growthRepo.save(rec);

        return toResponse(rec, baby);
    }

    @Transactional(readOnly = true)
    public List<GrowthResponse> list(UUID babyId, UUID userId) {
        checkMembership(babyId, userId);
        Baby baby = babyRepo.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return growthRepo.findByBabyIdOrderByMeasurementDateDesc(babyId)
                .stream().map(r -> toResponse(r, baby)).toList();
    }

    private GrowthResponse toResponse(GrowthRecord r, Baby baby) {
        String sex = baby.getGender() != null ? baby.getGender() : "M";
        int ageMonths = ageInMonths(baby.getBirthDate(), r.getMeasurementDate());

        return new GrowthResponse(
                r.getId(),
                r.getMeasurementDate(),
                r.getWeightKg(),
                r.getHeightCm(),
                r.getHeadCm(),
                who.percentile(sex, "weight", ageMonths, r.getWeightKg()),
                who.percentile(sex, "height", ageMonths, r.getHeightCm()),
                who.percentile(sex, "head",   ageMonths, r.getHeadCm())
        );
    }

    private int ageInMonths(LocalDate birthDate, LocalDate measureDate) {
        return Period.between(birthDate, measureDate).toTotalMonths() < 0
                ? 0 : (int) Period.between(birthDate, measureDate).toTotalMonths();
    }
}
