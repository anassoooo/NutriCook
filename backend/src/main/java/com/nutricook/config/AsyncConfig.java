package com.nutricook.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded executors for AI work, so slow or stuck AI calls can never exhaust request threads
 * (BR-LIF-07, BR-AI-05).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

  /** Runs post-commit plan enrichment. */
  @Bean
  public Executor aiEnrichmentExecutor() {
    return executor("ai-enrich-", 2, 4, 100);
  }

  /** Runs individual AI calls so they can be abandoned on timeout. */
  @Bean
  public Executor aiCallExecutor() {
    return executor("ai-call-", 4, 8, 200);
  }

  private static ThreadPoolTaskExecutor executor(String prefix, int core, int max, int queue) {
    ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
    e.setThreadNamePrefix(prefix);
    e.setCorePoolSize(core);
    e.setMaxPoolSize(max);
    e.setQueueCapacity(queue);
    e.initialize();
    return e;
  }
}
