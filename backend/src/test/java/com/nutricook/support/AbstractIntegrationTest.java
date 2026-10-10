package com.nutricook.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests: full Spring context, MockMvc, a real PostgreSQL 16 (same engine
 * as production, Constitution V) and a scriptable {@link StubChatModel} instead of Groq.
 *
 * <p>The container is a JVM-wide singleton so every subclass shares one database and one cached
 * Spring context. Tests must therefore not rely on an empty database (use unique emails, etc.).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StubChatModelConfig.class)
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected StubChatModel chatModel;
}
