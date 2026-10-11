package com.nutricook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import com.nutricook.init.DemoDataInitializer;
import com.nutricook.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** The prod profile: no API docs, no demo admin, a checked JWT secret, CORS from configuration. */
@ActiveProfiles("prod") // added to "test", which provides a strong secret and the test database
@TestPropertySource(
    properties = {
      // the container connection replaces these; they only need to resolve
      "DATABASE_URL=jdbc:postgresql://unused/unused",
      "DATABASE_USERNAME=unused",
      "DATABASE_PASSWORD=unused",
      "app.cors.allowed-origins=https://nutricook.example"
    })
class ProdProfileIT extends AbstractIntegrationTest {

  @Autowired ApplicationContext context;

  @Test
  void apiDocsAreNotServed() throws Exception {
    for (String path : new String[] {"/swagger-ui.html", "/swagger-ui/index.html", "/api-docs"}) {
      assertThat(mockMvc.perform(get(path)).andReturn().getResponse().getStatus())
          .as(path)
          .isEqualTo(404);
    }
  }

  @Test
  void secretIsCheckedAndNoDemoAdminExists() {
    assertThat(context.getBeansOfType(JwtSecretValidator.class)).hasSize(1);
    assertThat(context.getBeansOfType(DemoDataInitializer.class)).isEmpty();
  }

  @Test
  void onlyConfiguredOriginsMayCallTheApi() throws Exception {
    var allowed =
        mockMvc
            .perform(
                options("/api/auth/login")
                    .header("Origin", "https://nutricook.example")
                    .header("Access-Control-Request-Method", "POST"))
            .andReturn()
            .getResponse();
    assertThat(allowed.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("https://nutricook.example");

    var other =
        mockMvc
            .perform(
                options("/api/auth/login")
                    .header("Origin", "https://evil.example")
                    .header("Access-Control-Request-Method", "POST"))
            .andReturn()
            .getResponse();
    assertThat(other.getStatus()).isEqualTo(403);
    assertThat(other.getHeader("Access-Control-Allow-Origin")).isNull();
  }
}
