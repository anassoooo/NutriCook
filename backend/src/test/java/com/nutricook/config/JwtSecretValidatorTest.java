package com.nutricook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** A production deploy fails fast with a weak or published JWT secret. */
class JwtSecretValidatorTest {

  private static String base64Of(int bytes) {
    return Base64.getEncoder().encodeToString(new byte[bytes]);
  }

  @Test
  void acceptsARandom48ByteSecret() {
    assertThat(JwtSecretValidator.problems(base64Of(48))).isEmpty();
  }

  @Test
  void rejectsMissingShortInvalidAndPublishedSecrets() {
    assertThat(JwtSecretValidator.problems("")).containsExactly("it is not set");
    assertThat(JwtSecretValidator.problems(base64Of(16)))
        .containsExactly("it decodes to 16 bytes, at least 32 are needed");
    assertThat(JwtSecretValidator.problems("not base64 at all!"))
        .contains("it is not valid base64");
    for (String published : JwtSecretValidator.KNOWN_DEV_SECRETS) {
      assertThat(JwtSecretValidator.problems(published)).contains("it is a published dev default");
    }
  }

  @Test
  void failsStartupWithoutEchoingTheSecret() {
    String weak = base64Of(8);
    assertThatThrownBy(() -> new JwtSecretValidator(weak))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not safe for production")
        .hasMessageNotContaining(weak);
  }
}
