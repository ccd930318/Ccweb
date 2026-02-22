package com.ccweb.babytracker.vaccination;

import com.ccweb.babytracker.vaccination.dto.ScheduleResponse;
import com.ccweb.babytracker.vaccination.dto.VaccinationRequest;
import com.ccweb.babytracker.vaccination.dto.VaccinationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/api/vaccines/schedule")
    public List<ScheduleResponse> listSchedule(Authentication auth) {
        return vaccinationService.listSchedule();
    }

    @PostMapping("/api/babies/{babyId}/vaccinations")
    @ResponseStatus(HttpStatus.CREATED)
    public VaccinationResponse record(@PathVariable UUID babyId,
                                      @Valid @RequestBody VaccinationRequest req,
                                      Authentication auth) {
        return vaccinationService.record(babyId, UUID.fromString(auth.getName()), req);
    }

    @GetMapping("/api/babies/{babyId}/vaccinations")
    public List<VaccinationResponse> list(@PathVariable UUID babyId, Authentication auth) {
        return vaccinationService.list(babyId, UUID.fromString(auth.getName()));
    }
}
