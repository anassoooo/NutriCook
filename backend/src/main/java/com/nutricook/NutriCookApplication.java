package com.nutricook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NutriCookApplication {

  public static void main(String[] args) {
    SpringApplication.run(NutriCookApplication.class, args);
  }
}
