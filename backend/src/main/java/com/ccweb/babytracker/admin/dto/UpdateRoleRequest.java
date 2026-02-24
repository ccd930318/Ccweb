package com.ccweb.babytracker.admin.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
    @Pattern(regexp = "USER|ADMIN", message = "Role must be USER or ADMIN")
    String role
) {}
