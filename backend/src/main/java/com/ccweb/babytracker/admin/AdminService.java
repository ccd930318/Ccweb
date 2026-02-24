package com.ccweb.babytracker.admin;

import com.ccweb.babytracker.admin.dto.StatsDto;
import com.ccweb.babytracker.admin.dto.UpdateRoleRequest;
import com.ccweb.babytracker.admin.dto.UserDto;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final BabyRepository babyRepository;

    public AdminService(UserRepository userRepository, BabyRepository babyRepository) {
        this.userRepository = userRepository;
        this.babyRepository = babyRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StatsDto getStats() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .count();
        long totalBabies = babyRepository.count();
        return new StatsDto(totalUsers, adminCount, totalBabies);
    }

    public UserDto updateRole(UUID userId, UpdateRoleRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(req.role());
        userRepository.save(user);
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
