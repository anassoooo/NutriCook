package com.nutricook.init;

import com.nutricook.entity.User;
import com.nutricook.entity.UserProfile;
import com.nutricook.entity.enums.Role;
import com.nutricook.repository.UserProfileRepository;
import com.nutricook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the demo admin account. Reference data (foods, restrictions, conditions, achievements) is
 * owned by Flyway migrations (Constitution VI). Task T079 restricts this to the local profile with
 * credentials from the environment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

  private final UserRepository userRepo;
  private final UserProfileRepository profileRepo;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (userRepo.existsByEmail("admin@nutricook.dev")) return;
    User admin =
        User.builder()
            .email("admin@nutricook.dev")
            .passwordHash(passwordEncoder.encode("Admin1234!"))
            .role(Role.ADMIN)
            .build();
    userRepo.save(admin);
    profileRepo.save(UserProfile.builder().user(admin).build());
    log.info("Demo admin user created: admin@nutricook.dev");
  }
}
