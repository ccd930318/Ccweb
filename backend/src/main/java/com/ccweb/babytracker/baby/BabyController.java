package com.ccweb.babytracker.baby;

import com.ccweb.babytracker.baby.dto.BabyRequest;
import com.ccweb.babytracker.baby.dto.BabyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/babies")
public class BabyController {

    private final BabyService babyService;

    public BabyController(BabyService babyService) {
        this.babyService = babyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BabyResponse create(@Valid @RequestBody BabyRequest req, Authentication auth) {
        return babyService.create(req, UUID.fromString(auth.getName()));
    }

    @GetMapping
    public List<BabyResponse> list(Authentication auth) {
        return babyService.listForUser(UUID.fromString(auth.getName()));
    }

    @GetMapping("/{id}")
    public BabyResponse getById(@PathVariable UUID id, Authentication auth) {
        return babyService.getById(id, UUID.fromString(auth.getName()));
    }
}
