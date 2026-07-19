package com.projectkinetile.physicsengine.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Hardened Jackson defaults for Kafka and REST JSON parsing.
 */
@Configuration
public class JacksonConfig {

  /**
   * Rejects unknown JSON properties to limit malicious or malformed payloads.
   *
   * @return Jackson builder customizer
   */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer failOnUnknownPropertiesCustomizer() {
    return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}
