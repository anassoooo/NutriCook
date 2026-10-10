package com.nutricook.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nutricook.domain.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The frontend must know every error code the API can return (Constitution VIII). Reads the
 * ERROR_CODES list from frontend/src/lib/errors.ts.
 */
class ErrorCodeSyncTest {

  private static final Path FRONTEND_ERRORS = Path.of("..", "frontend", "src", "lib", "errors.ts");

  @Test
  void frontendErrorCodesMatchBackendEnum() throws Exception {
    assumeTrue(Files.exists(FRONTEND_ERRORS), "frontend sources not available");
    String source = Files.readString(FRONTEND_ERRORS);
    Matcher block =
        Pattern.compile("ERROR_CODES = \\[(.*?)\\] as const", Pattern.DOTALL).matcher(source);
    assertThat(block.find()).as("ERROR_CODES array in errors.ts").isTrue();

    var frontend =
        Pattern.compile("'([A-Z_]+)'")
            .matcher(block.group(1))
            .results()
            .map(m -> m.group(1))
            .collect(Collectors.toSet());
    var backend = Arrays.stream(ErrorCode.values()).map(Enum::name).collect(Collectors.toSet());

    assertThat(frontend).isEqualTo(backend);
  }
}
