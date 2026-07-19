package com.projectkinetile.physicsengine.api;

/**
 * Canonical REST path prefixes and route roots for the physics-engine HTTP API.
 *
 * <p>Keep filters, security matchers, and controllers aligned by referencing these constants
 * instead of duplicating path literals.</p>
 */
public final class ApiPaths {

  /** API version root (no trailing slash), e.g. {@code /api/v1}. */
  public static final String V1 = "/api/v1";

  /** Prefix for {@link String#startsWith} checks on request URIs ({@code /api/v1/}). */
  public static final String V1_URI_PREFIX = V1 + "/";

  /** Ant-style pattern for Spring Security matchers ({@code /api/v1/**}). */
  public static final String V1_ANT_PATTERN = V1 + "/**";

  /** CORS registration pattern covering all API versions ({@code /api/**}). */
  public static final String CORS_PATTERN = "/api/**";

  public static final String ENERGY = V1 + "/energy";
  public static final String CONFIG = V1 + "/config";
  public static final String CONFIG_HARDWARE = CONFIG + "/hardware";

  /** Tile monitoring thresholds for inventory staleness ({@code /api/v1/config/tile-monitoring}). */
  public static final String CONFIG_TILE_MONITORING = CONFIG + "/tile-monitoring";
  public static final String DEVICES = V1 + "/devices";
  public static final String INFRASTRUCTURE = V1 + "/infrastructure";

  private ApiPaths() {}

  /**
   * @param requestUri servlet request URI
   * @return {@code true} if the URI targets a v1 API endpoint
   */
  public static boolean isV1Request(String requestUri) {
    return requestUri != null && requestUri.startsWith(V1_URI_PREFIX);
  }
}
