package com.nutricook.service;

import com.nutricook.dto.request.LoginRequest;
import com.nutricook.dto.request.RegisterRequest;
import com.nutricook.dto.response.AuthResponse;
import com.nutricook.entity.User;
import com.nutricook.entity.UserProfile;
import com.nutricook.exception.DuplicateEmailException;
import com.nutricook.repository.UserProfileRepository;
import com.nutricook.repository.UserRepository;
import com.nutricook.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateEmailException(request.getEmail());
    }

    User user =
        User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();
    userRepository.save(user);

    UserProfile profile = UserProfile.builder().user(user).build();
    profileRepository.save(profile);

    return AuthResponse.builder()
        .userId(user.getId())
        .token(jwtService.generateToken(user))
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
    return AuthResponse.builder()
        .userId(user.getId())
        .token(jwtService.generateToken(user))
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
  }
}
