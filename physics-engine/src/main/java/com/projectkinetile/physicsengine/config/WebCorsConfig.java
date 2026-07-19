package com.projectkinetile.physicsengine.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.projectkinetile.physicsengine.api.ApiHeaders;
import com.projectkinetile.physicsengine.api.ApiPaths;

/**
 * Explicit CORS allowlist (no wildcard origins in production).
 */
@Configuration
public class WebCorsConfig {

  private final AppSecurityProperties appSecurityProperties;

  public WebCorsConfig(AppSecurityProperties appSecurityProperties) {
    this.appSecurityProperties = appSecurityProperties;
  }

  /**
   * Registers CORS rules for {@code /api/**} from configured allowed origins.
   *
   * @return CORS configuration source
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> origins = appSecurityProperties.getCors().allowedOriginList();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "HEAD", "OPTIONS"));
    config.setAllowedHeaders(List.of(ApiHeaders.CONTENT_TYPE));
    config.setMaxAge(3600L); // 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(ApiPaths.CORS_PATTERN, config);
    return source;
  }
}
