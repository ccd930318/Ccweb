package com.ccweb.babytracker.growth;

import com.ccweb.babytracker.growth.dto.GrowthRequest;
import com.ccweb.babytracker.growth.dto.GrowthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/babies/{babyId}/growth")
public class GrowthController {

    private final GrowthService growthService;

    public GrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrowthResponse create(@PathVariable UUID babyId,
                                 @Valid @RequestBody GrowthRequest req,
                                 Authentication auth) {
        return growthService.create(babyId, UUID.fromString(auth.getName()), req);
    }

    @GetMapping
    public List<GrowthResponse> list(@PathVariable UUID babyId, Authentication auth) {
        return growthService.list(babyId, UUID.fromString(auth.getName()));
    }
}
