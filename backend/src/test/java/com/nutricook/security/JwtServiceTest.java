package com.nutricook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutricook.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;

  // Valid base64 key (32 bytes) — test use only
  private static final String TEST_SECRET = "4JBzn+VDduZQFSMTvQYPUj0O78ZjVjIPdf2kRGvppxY=";
  private static final long ONE_HOUR_MS = 3_600_000L;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
    ReflectionTestUtils.setField(jwtService, "expirationMs", ONE_HOUR_MS);
  }

  @Test
  void generateToken_extractEmail_roundTrip() {
    User user = user("alice@example.com");
    String token = jwtService.generateToken(user);
    assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
  }

  @Test
  void isTokenValid_trueForCorrectUser() {
    User user = user("bob@example.com");
    String token = jwtService.generateToken(user);
    assertThat(jwtService.isTokenValid(token, user)).isTrue();
  }

  @Test
  void isTokenValid_falseForDifferentUser() {
    User owner = user("owner@example.com");
    User other = user("other@example.com");
    String token = jwtService.generateToken(owner);
    assertThat(jwtService.isTokenValid(token, other)).isFalse();
  }

  @Test
  void isTokenValid_falseForTamperedToken() {
    User user = user("carol@example.com");
    String token = jwtService.generateToken(user);
    String tampered = token.substring(0, token.length() - 4) + "XXXX";
    assertThatThrownBy(() -> jwtService.isTokenValid(tampered, user)).isInstanceOf(Exception.class);
  }

  @Test
  void generateToken_producesThreePartJwt() {
    String token = jwtService.generateToken(user("dave@example.com"));
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  void generateToken_containsSubjectClaim() {
    String token = jwtService.generateToken(user("eve@example.com"));
    // JWT payload is the middle segment (base64url-encoded JSON)
    String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    assertThat(payload).contains("eve@example.com");
  }

  // ── helper ───────────────────────────────────────────────────────────

  private User user(String email) {
    return User.builder().email(email).passwordHash("hashed").build();
  }
}
