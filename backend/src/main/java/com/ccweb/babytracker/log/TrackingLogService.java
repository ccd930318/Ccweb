package com.ccweb.babytracker.log;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.log.TrackingLog;
import com.ccweb.babytracker.domain.log.TrackingLogRepository;
import com.ccweb.babytracker.log.dto.LogRequest;
import com.ccweb.babytracker.log.dto.LogResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TrackingLogService {

    private final TrackingLogRepository logRepository;
    private final BabyRepository babyRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public TrackingLogService(TrackingLogRepository logRepository,
                              BabyRepository babyRepository,
                              FamilyMemberRepository familyMemberRepository) {
        this.logRepository = logRepository;
        this.babyRepository = babyRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    private void checkMembership(UUID babyId, UUID userId) {
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(babyId);
        fmId.setUserId(userId);
        if (!familyMemberRepository.existsById(fmId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public LogResponse create(UUID babyId, UUID userId, LogRequest req) {
        checkMembership(babyId, userId);
        Baby baby = babyRepository.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        TrackingLog log = new TrackingLog();
        log.setBaby(baby);
        log.setType(req.type());
        log.setOccurredAt(req.occurredAt());
        log.setNotes(req.notes());
        logRepository.save(log);

        return toResponse(log);
    }

    @Transactional(readOnly = true)
    public List<LogResponse> list(UUID babyId, UUID userId) {
        checkMembership(babyId, userId);
        return logRepository.findByBabyIdOrderByOccurredAtDesc(babyId)
                .stream().map(this::toResponse).toList();
    }

    private LogResponse toResponse(TrackingLog l) {
        return new LogResponse(l.getId(), l.getType(), l.getOccurredAt(), l.getNotes());
    }
}
