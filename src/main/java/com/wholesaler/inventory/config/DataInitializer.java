package com.wholesaler.inventory.config;

import com.wholesaler.inventory.entity.User;
import com.wholesaler.inventory.enums.Role;
import com.wholesaler.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("abc");
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        String adminUsername = "Sonu";

        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' already exists. Skipping initialization.", adminUsername);
            return;
        }

        log.info("Creating default admin user...");

        User adminUser = User.builder()
                .username("Sonu")
                .password(passwordEncoder.encode("India@123"))
                .email("sonukr4024@.com")
                .fullName("Sonu Kumar")
                .role(Role.ROLE_ADMIN)
                .isLocked(false)
                .failedLoginAttempts(0)
                .isInvited(false)  // Admin is the initial user, not invited
                .build();

        userRepository.save(adminUser);

        log.info("========================================================");
        log.info("Default admin user created successfully!");
        log.info("Username: {}", adminUser.getUsername());
        log.info("Email: {}", adminUser.getEmail());
        log.info("Role: {}", adminUser.getRole());
        log.info("========================================================");
        log.info("IMPORTANT: Only this admin can invite new users via /api/users/register");
        log.info("All other users must be invited by an admin to access the system");
        log.info("========================================================");
    }
}
