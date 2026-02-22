package com.ccweb.babytracker.food;

import com.ccweb.babytracker.food.dto.FoodItemResponse;
import com.ccweb.babytracker.food.dto.FoodLogRequest;
import com.ccweb.babytracker.food.dto.FoodLogResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/api/foods")
    public List<FoodItemResponse> catalogue(Authentication auth) {
        return foodService.catalogue();
    }

    @PostMapping("/api/babies/{babyId}/food-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodLogResponse log(@PathVariable UUID babyId,
                               @Valid @RequestBody FoodLogRequest req,
                               Authentication auth) {
        return foodService.log(babyId, UUID.fromString(auth.getName()), req);
    }

    @GetMapping("/api/babies/{babyId}/food-logs")
    public List<FoodLogResponse> listLogs(@PathVariable UUID babyId, Authentication auth) {
        return foodService.listLogs(babyId, UUID.fromString(auth.getName()));
    }
}
