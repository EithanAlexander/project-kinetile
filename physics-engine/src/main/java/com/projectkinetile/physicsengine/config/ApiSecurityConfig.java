package com.projectkinetile.physicsengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.api.ApiPaths;
import com.projectkinetile.physicsengine.security.ClientIpResolver;
import com.projectkinetile.physicsengine.security.RateLimitFilter;
import com.projectkinetile.physicsengine.service.RateLimitService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security: security headers, CORS, and rate-limit filter ordering for the open read-only
 * analytics API.
 *
 * <p>The API is intentionally unauthenticated: it serves public, read-only, non-PII energy
 * analytics. Abuse is bounded by the per-IP {@link RateLimitFilter}, strict CORS, and request
 * validation in the controllers. CSRF protection stays enabled: the surface is GET-only, sessions
 * are stateless, and there is no cookie-based authentication. Any non-{@code /api/v1/**} route is
 * denied except actuator info and OpenAPI documentation endpoints.
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(ApiSecurityConfig.class);

  private final AppSecurityProperties appSecurityProperties;

  public ApiSecurityConfig(AppSecurityProperties appSecurityProperties) {
    this.appSecurityProperties = appSecurityProperties;
  }

  @Bean
  public RateLimitFilter rateLimitFilter(
      RateLimitService rateLimitService, ObjectMapper objectMapper) {
    return new RateLimitFilter(appSecurityProperties, rateLimitService, objectMapper);
  }

  /**
   * Configures stateless security for the read-only analytics API.
   *
   * @param http security builder
   * @param rateLimitFilter per-IP rate limit filter
   * @return filter chain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter)
      throws Exception {
    http
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults())
        .headers(
            headers ->
                headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
        .exceptionHandling(
            exceptions ->
                exceptions.accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                      if (log.isWarnEnabled()) {
                        log.warn(
                            "API access denied: {} {} from {} ({})",
                            request.getMethod(),
                            request.getRequestURI(),
                            ClientIpResolver.resolveClientIp(request),
                            accessDeniedException.getMessage());
                      }
                      response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll();
              auth.requestMatchers(
                      "/v3/api-docs/**",
                      "/swagger-ui.html",
                      "/swagger-ui/**")
                  .permitAll();
              auth.requestMatchers(HttpMethod.GET, ApiPaths.V1_ANT_PATTERN).permitAll();
              auth.anyRequest().denyAll();
            });

    if (appSecurityProperties.getRateLimit().isEnabled()) {
      http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
    }

    if (log.isInfoEnabled()) {
      log.info(
          "REST security configured: auth=none (open read-only API), rateLimitEnabled={}"
              + " ({} req/min), corsOrigins={}",
          appSecurityProperties.getRateLimit().isEnabled(),
          appSecurityProperties.getRateLimit().getRequestsPerMinute(),
          appSecurityProperties.getCors().allowedOriginList().size());
    }

    return http.build();
  }
}
