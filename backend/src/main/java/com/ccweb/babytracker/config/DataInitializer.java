package com.ccweb.babytracker.config;

import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_EMAIL = "admin@ccweb.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String ADMIN_NAME = "系統管理員";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setName(ADMIN_NAME);
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("預設管理員帳號已建立: {}", ADMIN_EMAIL);
        }
    }
}
