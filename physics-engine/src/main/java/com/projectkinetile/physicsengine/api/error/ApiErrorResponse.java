package com.projectkinetile.physicsengine.api.error;

import java.util.List;

/**
 * Sanitized error body returned to API clients (no stack traces or SQL details).
 *
 * @param status HTTP status code
 * @param error short error category
 * @param message safe user-facing message
 * @param fieldErrors optional validation field errors
 */
public record ApiErrorResponse(
    int status, String error, String message, List<FieldError> fieldErrors) {

  /** Single field validation failure. */
  public record FieldError(String field, String message) {}

  public static ApiErrorResponse of(int status, String error, String message) {
    return new ApiErrorResponse(status, error, message, List.of());
  }
}
