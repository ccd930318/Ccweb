package com.ccweb.babytracker.invite.dto;

import java.util.UUID;

public record InviteResponse(UUID id, String token, String role, String inviteeEmail) {}
