package com.nutricook.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the Groq-backed model with {@link StubChatModel} in integration tests. */
@TestConfiguration(proxyBeanMethods = false)
public class StubChatModelConfig {

  @Bean
  @Primary
  StubChatModel stubChatModel() {
    return new StubChatModel();
  }
}
