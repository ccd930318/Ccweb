package com.ccweb.babytracker.vaccination;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.vaccination.*;
import com.ccweb.babytracker.vaccination.dto.ScheduleResponse;
import com.ccweb.babytracker.vaccination.dto.VaccinationRequest;
import com.ccweb.babytracker.vaccination.dto.VaccinationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VaccinationService {

    private final VaccinationRecordRepository recordRepo;
    private final VaccineScheduleRepository scheduleRepo;
    private final BabyRepository babyRepo;
    private final FamilyMemberRepository familyMemberRepo;

    public VaccinationService(VaccinationRecordRepository recordRepo,
                              VaccineScheduleRepository scheduleRepo,
                              BabyRepository babyRepo,
                              FamilyMemberRepository familyMemberRepo) {
        this.recordRepo = recordRepo;
        this.scheduleRepo = scheduleRepo;
        this.babyRepo = babyRepo;
        this.familyMemberRepo = familyMemberRepo;
    }

    private void checkMembership(UUID babyId, UUID userId) {
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(babyId);
        fmId.setUserId(userId);
        if (!familyMemberRepo.existsById(fmId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedule() {
        return scheduleRepo.findAllByOrderByRecommendedAgeMonthsAscDoseNumberAsc()
                .stream().map(s -> new ScheduleResponse(s.getId(), s.getVaccineName(),
                        s.getDoseNumber(), s.getRecommendedAgeMonths(), s.getNotes())).toList();
    }

    public VaccinationResponse record(UUID babyId, UUID userId, VaccinationRequest req) {
        checkMembership(babyId, userId);
        Baby baby = babyRepo.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        VaccineSchedule schedule = scheduleRepo.findById(req.scheduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown schedule"));

        VaccinationRecord rec = new VaccinationRecord();
        rec.setBaby(baby);
        rec.setSchedule(schedule);
        rec.setAdministeredDate(req.administeredDate());
        rec.setNotes(req.notes());
        recordRepo.save(rec);

        return toResponse(rec);
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> list(UUID babyId, UUID userId) {
        checkMembership(babyId, userId);
        return recordRepo.findByBabyIdOrderByAdministeredDateDesc(babyId)
                .stream().map(this::toResponse).toList();
    }

    private VaccinationResponse toResponse(VaccinationRecord r) {
        return new VaccinationResponse(r.getId(),
                r.getSchedule().getVaccineName(),
                r.getSchedule().getDoseNumber(),
                r.getAdministeredDate(),
                r.getNotes());
    }
}
