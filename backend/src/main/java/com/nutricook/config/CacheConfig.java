package com.nutricook.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Value("${app.cache.location-ttl-minutes:60}")
  private long locationTtlMinutes;

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager("locations");
    manager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(locationTtlMinutes, TimeUnit.MINUTES)
            .maximumSize(500));
    return manager;
  }
}
