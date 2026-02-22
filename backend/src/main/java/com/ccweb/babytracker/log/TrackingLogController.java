package com.ccweb.babytracker.log;

import com.ccweb.babytracker.log.dto.LogRequest;
import com.ccweb.babytracker.log.dto.LogResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/babies/{babyId}/logs")
public class TrackingLogController {

    private final TrackingLogService logService;

    public TrackingLogController(TrackingLogService logService) {
        this.logService = logService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogResponse create(@PathVariable UUID babyId,
                              @Valid @RequestBody LogRequest req,
                              Authentication auth) {
        return logService.create(babyId, UUID.fromString(auth.getName()), req);
    }

    @GetMapping
    public List<LogResponse> list(@PathVariable UUID babyId, Authentication auth) {
        return logService.list(babyId, UUID.fromString(auth.getName()));
    }
}
