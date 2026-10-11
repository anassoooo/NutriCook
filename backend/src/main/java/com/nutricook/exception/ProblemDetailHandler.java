package com.nutricook.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nutricook.config.CorrelationIdFilter;
import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every failure into an RFC 9457 {@code ProblemDetail} with a stable {@code code}
 * (business-rules.md §12, Constitution VII). Messages from unexpected exceptions are never sent to
 * clients; they are logged with a reference id instead.
 */
@Slf4j
@RestControllerAdvice
public class ProblemDetailHandler {

  /** HTTP status for each business code. The switch is exhaustive: a new code must be mapped. */
  public static HttpStatus statusOf(ErrorCode code) {
    return switch (code) {
      case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case EMAIL_TAKEN, PLAN_READ_ONLY, FOOD_READ_ONLY -> HttpStatus.CONFLICT;
      case PROFILE_INCOMPLETE, PROFILE_INELIGIBLE, PLAN_INFEASIBLE, SWAP_REJECTED -> HttpStatus
          .UNPROCESSABLE_ENTITY;
      case GENERATION_LIMIT, RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      case AI_UNAVAILABLE, MAP_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  /** Builds the standard body; shared with the security entry points. */
  public static ProblemDetail problem(
      ErrorCode code, HttpStatusCode status, String detail, String path) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create("urn:nutricook:problem:" + code.name().toLowerCase(Locale.ROOT)));
    pd.setTitle(titleOf(code));
    if (path != null) pd.setInstance(URI.create(path));
    pd.setProperty("code", code.name());
    pd.setProperty("timestamp", Instant.now().toString());
    String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
    if (requestId != null) pd.setProperty("requestId", requestId);
    return pd;
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest req) {
    HttpStatus status = statusOf(ex.code());
    ProblemDetail pd = problem(ex.code(), status, ex.getMessage(), req.getRequestURI());
    ex.details().forEach(pd::setProperty);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleBodyValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
    return validationFailed("Some fields are invalid.", errors, req);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ProblemDetail> handleParamValidation(
      HandlerMethodValidationException ex, HttpServletRequest req) {
    List<Map<String, String>> errors =
        ex.getAllValidationResults().stream()
            .flatMap(
                r ->
                    r.getResolvableErrors().stream()
                        .map(
                            e ->
                                fieldError(
                                    r.getMethodParameter().getParameterName(),
                                    e.getDefaultMessage())))
            .toList();
    return validationFailed("Some parameters are invalid.", errors, req);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    List<Map<String, String>> errors =
        ex.getConstraintViolations().stream()
            .map(v -> fieldError(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return validationFailed("Some fields are invalid.", errors, req);
  }

  /** Unreadable JSON, or a value of the wrong type (e.g. an unknown enum constant). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
      String field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
      ResponseEntity<ProblemDetail> response =
          validationFailed(
              "Invalid value for " + field + ".",
              List.of(fieldError(field, "has an invalid value")),
              req);
      Class<?> target = ife.getTargetType();
      if (target != null && target.isEnum()) {
        response
            .getBody()
            .setProperty(
                "allowedValues",
                Arrays.stream(target.getEnumConstants()).map(Object::toString).toList());
      }
      return response;
    }
    return validationFailed("The request body is not valid JSON.", List.of(), req);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    return validationFailed(
        "Invalid value for " + ex.getName() + ".",
        List.of(fieldError(ex.getName(), "has an invalid value")),
        req);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthentication(
      AuthenticationException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            problem(
                ErrorCode.UNAUTHENTICATED,
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password.",
                req.getRequestURI()));
  }

  /** Thrown by {@code @PreAuthorize} ownership/role checks. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            problem(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "You do not have access to this resource.",
                req.getRequestURI()));
  }

  /**
   * Other Spring MVC exceptions (unknown route, wrong method, missing parameter, unsupported media
   * type) implement {@link ErrorResponse}: keep their status, attach a business code.
   */
  private ResponseEntity<ProblemDetail> handleFramework(ErrorResponse ex, HttpServletRequest req) {
    HttpStatusCode status = ex.getStatusCode();
    ErrorCode code = status.value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.VALIDATION_FAILED;
    String detail =
        status.value() == 404
            ? "No such resource."
            : "The request is malformed (" + status.value() + ").";
    return ResponseEntity.status(status).body(problem(code, status, detail, req.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
    if (ex instanceof ErrorResponse framework) return handleFramework(framework, req);
    String reference = UUID.randomUUID().toString();
    log.error(
        "Unhandled error [ref={}] on {} {}", reference, req.getMethod(), req.getRequestURI(), ex);
    ProblemDetail pd =
        problem(
            ErrorCode.INTERNAL,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong on our side. Please try again.",
            req.getRequestURI());
    pd.setProperty("reference", reference);
    return ResponseEntity.internalServerError().body(pd);
  }

  private ResponseEntity<ProblemDetail> validationFailed(
      String detail, List<Map<String, String>> errors, HttpServletRequest req) {
    ProblemDetail pd =
        problem(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, detail, req.getRequestURI());
    pd.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(pd);
  }

  private static Map<String, String> fieldError(String field, String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("field", field);
    m.put("message", message);
    return m;
  }

  private static String titleOf(ErrorCode code) {
    return switch (code) {
      case VALIDATION_FAILED -> "Validation failed";
      case UNAUTHENTICATED -> "Authentication required";
      case FORBIDDEN -> "Forbidden";
      case NOT_FOUND -> "Not found";
      case EMAIL_TAKEN -> "Email already registered";
      case PLAN_READ_ONLY -> "Plan is read-only";
      case FOOD_READ_ONLY -> "Food is read-only";
      case PROFILE_INCOMPLETE -> "Profile incomplete";
      case PROFILE_INELIGIBLE -> "Profile not eligible";
      case PLAN_INFEASIBLE -> "No safe plan possible";
      case SWAP_REJECTED -> "Swap rejected";
      case GENERATION_LIMIT -> "Daily generation limit reached";
      case RATE_LIMITED -> "Too many requests";
      case AI_UNAVAILABLE -> "AI assistant unavailable";
      case MAP_UNAVAILABLE -> "Map data unavailable";
      case INTERNAL -> "Internal error";
    };
  }
}
