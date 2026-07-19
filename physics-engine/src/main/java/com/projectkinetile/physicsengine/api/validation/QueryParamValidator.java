package com.projectkinetile.physicsengine.api.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates untrusted REST query parameters before database access.
 */
public final class QueryParamValidator {

  private QueryParamValidator() {}

  /**
   * Rejects query string values that exceed the configured maximum length.
   *
   * @param value raw parameter value (may be null)
   * @param paramName parameter name for error messages
   * @param maxLength inclusive upper bound
   */
  public static void requireMaxLength(String value, String paramName, int maxLength) {
    if (value != null && value.length() > maxLength) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, paramName + " exceeds maximum length of " + maxLength);
    }
  }

  /**
   * Rejects ledger sort strings that do not match {@code property,direction} with bounded length.
   *
   * @param sort sort parameter
   * @param maxLength maximum allowed length
   */
  public static void requireValidSortParam(String sort, int maxLength) {
    if (sort == null || sort.isBlank()) {
      return;
    }
    if (sort.length() > maxLength) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort exceeds maximum length");
    }
    if (!sort.matches("^[a-zA-Z0-9_,\\-]{1,64}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort has invalid format");
    }
  }
}
