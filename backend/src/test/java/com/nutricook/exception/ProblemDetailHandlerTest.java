package com.nutricook.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** The HTTP contract of every error code (business-rules.md §12). */
class ProblemDetailHandlerTest {

  /** Copy of the catalogue table. If this test fails, the catalogue and code disagree. */
  private static final Map<ErrorCode, Integer> CATALOGUE = new EnumMap<>(ErrorCode.class);

  static {
    CATALOGUE.put(ErrorCode.VALIDATION_FAILED, 400);
    CATALOGUE.put(ErrorCode.UNAUTHENTICATED, 401);
    CATALOGUE.put(ErrorCode.FORBIDDEN, 403);
    CATALOGUE.put(ErrorCode.NOT_FOUND, 404);
    CATALOGUE.put(ErrorCode.EMAIL_TAKEN, 409);
    CATALOGUE.put(ErrorCode.PLAN_READ_ONLY, 409);
    CATALOGUE.put(ErrorCode.FOOD_READ_ONLY, 409);
    CATALOGUE.put(ErrorCode.PROFILE_INCOMPLETE, 422);
    CATALOGUE.put(ErrorCode.PROFILE_INELIGIBLE, 422);
    CATALOGUE.put(ErrorCode.PLAN_INFEASIBLE, 422);
    CATALOGUE.put(ErrorCode.SWAP_REJECTED, 422);
    CATALOGUE.put(ErrorCode.GENERATION_LIMIT, 429);
    CATALOGUE.put(ErrorCode.RATE_LIMITED, 429);
    CATALOGUE.put(ErrorCode.AI_UNAVAILABLE, 503);
    CATALOGUE.put(ErrorCode.MAP_UNAVAILABLE, 503);
    CATALOGUE.put(ErrorCode.INTERNAL, 500);
  }

  private final ProblemDetailHandler handler = new ProblemDetailHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/x");

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void everyCodeMapsToItsCatalogueStatusAndCarriesCodeAndDetails(ErrorCode code) {
    ResponseEntity<ProblemDetail> response =
        handler.handleDomain(
            new DomainException(code, "Readable message.", Map.of("constraint", "X")), request);

    assertThat(response.getStatusCode().value()).isEqualTo(CATALOGUE.get(code));
    ProblemDetail body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getProperties()).containsEntry("code", code.name());
    assertThat(body.getProperties()).containsEntry("constraint", "X");
    assertThat(body.getDetail()).isEqualTo("Readable message.");
    assertThat(body.getInstance()).hasToString("/api/x");
  }

  @Test
  void catalogueCoversEveryCode() {
    assertThat(CATALOGUE.keySet()).containsExactlyInAnyOrder(ErrorCode.values());
  }

  @Test
  void unexpectedExceptionNeverLeaksItsMessage() {
    ResponseEntity<ProblemDetail> response =
        handler.handleUnexpected(
            new IllegalStateException("SELECT * FROM users -- secret detail"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    ProblemDetail body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getProperties()).containsEntry("code", "INTERNAL").containsKey("reference");
    assertThat(body.toString()).doesNotContain("secret").doesNotContain("SELECT");
  }
}
