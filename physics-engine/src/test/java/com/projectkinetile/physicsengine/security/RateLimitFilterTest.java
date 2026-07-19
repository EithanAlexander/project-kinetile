package com.projectkinetile.physicsengine.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.api.ApiPaths;
import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rate limit filter")
class RateLimitFilterTest {

  @Mock private RateLimitService rateLimitService;

  private RateLimitFilter filter;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    AppSecurityProperties properties = new AppSecurityProperties();
    properties.getRateLimit().setEnabled(true);
    properties.getRateLimit().setRequestsPerMinute(60);
    filter = new RateLimitFilter(properties, rateLimitService, new ObjectMapper());
    chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);
  }

  @Test
  @DisplayName("delegates to rate limit service for v1 requests")
  void v1Request_delegatesToService() throws Exception {
    when(rateLimitService.tryConsume("10.0.0.1")).thenReturn(true);
    MockHttpServletRequest request = v1Request("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(rateLimitService).tryConsume("10.0.0.1");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("returns 429 when service denies the request")
  void v1Request_serviceDenies_returns429() throws Exception {
    when(rateLimitService.tryConsume("10.0.0.2")).thenReturn(false);
    MockHttpServletRequest request = v1Request("10.0.0.2");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
  }

  @Test
  @DisplayName("passes through non-v1 requests without calling the service")
  void nonV1Request_skipsService() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
    request.setRemoteAddr("10.0.0.3");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verifyNoInteractions(rateLimitService);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("does not invoke filter chain when rate limited")
  void rateLimitedRequest_doesNotInvokeFilterChain() throws Exception {
    when(rateLimitService.tryConsume("10.0.0.4")).thenReturn(false);
    var chainCalls = new int[1];
    FilterChain countingChain =
        (req, res) -> {
          chainCalls[0]++;
          ((HttpServletResponse) res).setStatus(200);
        };
    MockHttpServletRequest request = v1Request("10.0.0.4");

    filter.doFilter(request, new MockHttpServletResponse(), countingChain);

    assertThat(chainCalls[0]).isZero();
  }

  private static MockHttpServletRequest v1Request(@NonNull String remoteAddr) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", ApiPaths.ENERGY + "/ping");
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
