package com.projectkinetile.physicsengine.security;

import org.springframework.lang.NonNull;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the originating client IP from a servlet request (proxy-aware). */
public final class ClientIpResolver {

  private static final String UNKNOWN_CLIENT = "unknown";

  private ClientIpResolver() {}

  /**
   * Uses the left-most {@code X-Forwarded-For} hop when present; otherwise {@code remoteAddr}.
   *
   * @param request inbound HTTP request
   * @return stable client identifier for rate limiting and audit logs (never null)
   */
  @NonNull
  public static String resolveClientIp(@NonNull HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String firstHop = forwarded.split(",")[0].trim();
      if (!firstHop.isEmpty()) {
        return firstHop;
      }
    }
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : UNKNOWN_CLIENT;
  }
}
