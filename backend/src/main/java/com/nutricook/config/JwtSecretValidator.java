package com.nutricook.config;

import io.jsonwebtoken.io.Decoders;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in production with a weak or publicly known JWT secret: anyone who knows the
 * secret can sign a token for any user, admins included.
 */
@Component
@Profile("prod")
public class JwtSecretValidator {

  /** HS256 needs at least 256 bits. */
  static final int MIN_BYTES = 32;

  /** Secrets published in this repository (application.yml and .env.example). */
  static final Set<String> KNOWN_DEV_SECRETS =
      Set.of(
          "bnV0cmljb29rLWxvY2FsLWRldi1vbmx5LXNlY3JldC1DSEFOR0UtTUUtMDAwMQ==",
          "bnV0cmljb29rLWRvY2tlci1kZXYtb25seS1zZWNyZXQtQ0hBTkdFLU1FLTAwMDE=");

  public JwtSecretValidator(@Value("${app.jwt.secret:}") String secret) {
    List<String> problems = problems(secret);
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "JWT_SECRET is not safe for production: "
              + String.join("; ", problems)
              + ". Generate one with: openssl rand -base64 48");
    }
  }

  /** Every reason the secret is unsafe; empty when it is fine. Never echoes the secret. */
  static List<String> problems(String secret) {
    List<String> problems = new ArrayList<>();
    if (secret == null || secret.isBlank()) {
      problems.add("it is not set");
      return problems;
    }
    if (KNOWN_DEV_SECRETS.contains(secret.trim())) problems.add("it is a published dev default");
    try {
      int bytes = Decoders.BASE64.decode(secret.trim()).length;
      if (bytes < MIN_BYTES) {
        problems.add("it decodes to " + bytes + " bytes, at least " + MIN_BYTES + " are needed");
      }
    } catch (RuntimeException e) {
      problems.add("it is not valid base64");
    }
    return problems;
  }
}
