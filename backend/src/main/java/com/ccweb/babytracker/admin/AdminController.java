package com.ccweb.babytracker.admin;

import com.ccweb.babytracker.admin.dto.StatsDto;
import com.ccweb.babytracker.admin.dto.UpdateRoleRequest;
import com.ccweb.babytracker.admin.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserDto> listUsers() {
        return adminService.listUsers();
    }

    @GetMapping("/stats")
    public StatsDto getStats() {
        return adminService.getStats();
    }

    @PatchMapping("/users/{id}/role")
    public UserDto updateRole(@PathVariable UUID id,
                               @Valid @RequestBody UpdateRoleRequest req) {
        return adminService.updateRole(id, req);
    }
}
