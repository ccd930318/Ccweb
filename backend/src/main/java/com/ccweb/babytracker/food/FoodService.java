package com.ccweb.babytracker.food;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.food.*;
import com.ccweb.babytracker.food.dto.FoodItemResponse;
import com.ccweb.babytracker.food.dto.FoodLogRequest;
import com.ccweb.babytracker.food.dto.FoodLogResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FoodService {

    private final FoodLogRepository logRepo;
    private final FoodItemRepository itemRepo;
    private final BabyRepository babyRepo;
    private final FamilyMemberRepository familyMemberRepo;

    public FoodService(FoodLogRepository logRepo, FoodItemRepository itemRepo,
                       BabyRepository babyRepo, FamilyMemberRepository familyMemberRepo) {
        this.logRepo = logRepo;
        this.itemRepo = itemRepo;
        this.babyRepo = babyRepo;
        this.familyMemberRepo = familyMemberRepo;
    }

    private void checkMembership(UUID babyId, UUID userId) {
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(babyId);
        fmId.setUserId(userId);
        if (!familyMemberRepo.existsById(fmId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> catalogue() {
        return itemRepo.findAllByOrderByRecommendedAgeMonthsAscNameAsc()
                .stream().map(i -> new FoodItemResponse(i.getId(), i.getName(), i.getCategory(),
                        i.getRecommendedAgeMonths(), i.getNotes())).toList();
    }

    public FoodLogResponse log(UUID babyId, UUID userId, FoodLogRequest req) {
        checkMembership(babyId, userId);
        Baby baby = babyRepo.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        FoodItem item = itemRepo.findById(req.foodItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

        FoodLog fl = new FoodLog();
        fl.setBaby(baby);
        fl.setFoodItem(item);
        fl.setIntroducedDate(req.introducedDate());
        fl.setReaction(req.reaction());
        logRepo.save(fl);

        return toResponse(fl);
    }

    @Transactional(readOnly = true)
    public List<FoodLogResponse> listLogs(UUID babyId, UUID userId) {
        checkMembership(babyId, userId);
        return logRepo.findByBabyIdOrderByIntroducedDateDesc(babyId)
                .stream().map(this::toResponse).toList();
    }

    private FoodLogResponse toResponse(FoodLog fl) {
        return new FoodLogResponse(fl.getId(), fl.getFoodItem().getName(),
                fl.getFoodItem().getCategory(), fl.getIntroducedDate(), fl.getReaction());
    }
}
