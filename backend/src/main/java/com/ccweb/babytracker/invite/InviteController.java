package com.ccweb.babytracker.invite;

import com.ccweb.babytracker.invite.dto.InviteRequest;
import com.ccweb.babytracker.invite.dto.InviteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/api/babies/{babyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse createInvite(@PathVariable UUID babyId,
                                       @Valid @RequestBody InviteRequest req,
                                       Authentication auth) {
        return inviteService.createInvite(babyId, UUID.fromString(auth.getName()), req);
    }

    @PostMapping("/api/invites/{token}/accept")
    public void acceptInvite(@PathVariable String token, Authentication auth) {
        inviteService.acceptInvite(token, UUID.fromString(auth.getName()));
    }
}
