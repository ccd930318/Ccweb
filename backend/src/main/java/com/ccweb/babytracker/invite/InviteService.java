package com.ccweb.babytracker.invite;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMember;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.invite.Invite;
import com.ccweb.babytracker.domain.invite.InviteRepository;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import com.ccweb.babytracker.invite.dto.InviteRequest;
import com.ccweb.babytracker.invite.dto.InviteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class InviteService {

    private final InviteRepository inviteRepository;
    private final BabyRepository babyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public InviteService(InviteRepository inviteRepository,
                         BabyRepository babyRepository,
                         FamilyMemberRepository familyMemberRepository,
                         UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.babyRepository = babyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    public InviteResponse createInvite(UUID babyId, UUID requesterId, InviteRequest req) {
        // Verify requester is owner of the baby
        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(babyId);
        fmId.setUserId(requesterId);
        FamilyMember fm = familyMemberRepository.findById(fmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!"owner".equals(fm.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Baby baby = babyRepository.findById(babyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Invite invite = new Invite();
        invite.setBaby(baby);
        invite.setInviteeEmail(req.email());
        invite.setRole(req.role());
        invite.setToken(UUID.randomUUID().toString());
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        inviteRepository.save(invite);

        return new InviteResponse(invite.getId(), invite.getToken(), invite.getRole(), invite.getInviteeEmail());
    }

    public void acceptInvite(String token, UUID acceptorId) {
        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (invite.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite already accepted");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");
        }

        User user = userRepository.findById(acceptorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(invite.getBaby().getId());
        fmId.setUserId(acceptorId);

        if (!familyMemberRepository.existsById(fmId)) {
            FamilyMember fm = new FamilyMember();
            fm.setId(fmId);
            fm.setBaby(invite.getBaby());
            fm.setUser(user);
            fm.setRole(invite.getRole());
            familyMemberRepository.save(fm);
        }

        invite.setAccepted(true);
    }
}
