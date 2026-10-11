package com.nutricook.init;

import com.nutricook.entity.User;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.Role;
import com.nutricook.repository.UserProfileRepository;
import com.nutricook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a demo admin for local development and Docker Compose only, from {@code DEMO_ADMIN_EMAIL}
 * and {@code DEMO_ADMIN_PASSWORD}. Without a password nothing is created, so no deployment ever has
 * a default admin. Reference data is owned by Flyway (Constitution VI).
 */
@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

  private final UserRepository userRepo;
  private final UserProfileRepository profileRepo;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.demo.admin-email:admin@nutricook.local}")
  private String email;

  @Value("${app.demo.admin-password:}")
  private String password;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (password == null || password.isBlank()) {
      log.info("Demo admin not created: DEMO_ADMIN_PASSWORD is not set");
      return;
    }
    if (userRepo.existsByEmail(email)) return;
    User admin =
        User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .role(Role.ADMIN)
            .build();
    userRepo.save(admin);
    profileRepo.save(UserProfile.builder().user(admin).build());
    log.info("Demo admin created: {}", email); // never the password
  }
}
