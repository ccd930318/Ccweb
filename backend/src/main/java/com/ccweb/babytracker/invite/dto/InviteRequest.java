package com.ccweb.babytracker.invite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @Email @NotBlank String email,
        @NotBlank String role
) {}
