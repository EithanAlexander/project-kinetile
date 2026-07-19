package com.projectkinetile.physicsengine.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.api.ApiPaths;
import com.projectkinetile.physicsengine.api.error.ApiErrorResponse;
import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP filter that enforces Postgres-backed per-IP rate limits on {@link ApiPaths#V1_ANT_PATTERN}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  private final AppSecurityProperties appSecurityProperties;
  private final RateLimitService rateLimitService;
  private final ObjectMapper objectMapper;

  public RateLimitFilter(
      AppSecurityProperties appSecurityProperties,
      RateLimitService rateLimitService,
      ObjectMapper objectMapper) {
    this.appSecurityProperties = appSecurityProperties;
    this.rateLimitService = rateLimitService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (!appSecurityProperties.getRateLimit().isEnabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!ApiPaths.isV1Request(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = ClientIpResolver.resolveClientIp(request);
    if (!rateLimitService.tryConsume(clientIp)) {
      int limit = Math.max(1, appSecurityProperties.getRateLimit().getRequestsPerMinute());
      if (log.isWarnEnabled()) {
        log.warn(
            "Rate limit exceeded ({} req/min): {} {} from {}",
            limit,
            request.getMethod(),
            request.getRequestURI(),
            clientIp);
      }
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getOutputStream(),
          ApiErrorResponse.of(
              HttpStatus.TOO_MANY_REQUESTS.value(),
              "TOO_MANY_REQUESTS",
              "Rate limit exceeded. Try again later."));
      return;
    }

    filterChain.doFilter(request, response);
  }
}
