package com.ssg9th2team.geharbang.global.init;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCheckRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info(">>> Checking User ID 1...");
        User user = userRepository.findById(1L).orElse(null);
        if (user == null) {
            log.error(">>> User ID 1 does NOT exist! Creating it now...");
            try {
                User newUser = User.builder()
                        .email("test@example.com")
                        .password("password") // In real app, should be encoded
                        .phone("010-1234-5678")
                        .role(UserRole.USER) // Enum check required
                        .build();
                userRepository.save(newUser);
                log.info(">>> User ID 1 Created successfully!");
            } catch (Exception e) {
                log.error(">>> Failed to create User ID 1: ", e);
            }
        } else {
            log.info(">>> User ID 1 exists: {}", user.getEmail());
        }
    }
}
