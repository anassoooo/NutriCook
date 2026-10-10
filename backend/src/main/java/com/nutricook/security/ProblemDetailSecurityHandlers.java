package com.nutricook.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.domain.ErrorCode;
import com.nutricook.exception.ProblemDetailHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Filter-level security failures (no/invalid token, URL-level access denial) happen before the
 * DispatcherServlet, so {@link ProblemDetailHandler} never sees them. This writes the same
 * ProblemDetail body: 401 {@code UNAUTHENTICATED} instead of Spring's default empty 403.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailSecurityHandlers
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    write(response, request, ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "Please sign in.");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(
        response,
        request,
        ErrorCode.FORBIDDEN,
        HttpStatus.FORBIDDEN,
        "You do not have access to this resource.");
  }

  private void write(
      HttpServletResponse response,
      HttpServletRequest request,
      ErrorCode code,
      HttpStatus status,
      String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ProblemDetailHandler.problem(code, status, detail, request.getRequestURI()));
  }
}
