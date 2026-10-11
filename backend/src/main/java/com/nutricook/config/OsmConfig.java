package com.nutricook.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for OpenStreetMap's Overpass API. Bounded timeouts, so a slow or hung public server
 * can never hold a request thread for long.
 */
@Configuration(proxyBeanMethods = false)
public class OsmConfig {

  /** Overpass is asked to give up after this many seconds (see LocationService). */
  public static final int QUERY_TIMEOUT_SECONDS = 15;

  @Bean
  RestTemplate overpassRestTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(QUERY_TIMEOUT_SECONDS + 5L))
        .build();
  }
}
