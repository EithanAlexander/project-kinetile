package com.projectkinetile.physicsengine.api.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Query param validator")
class QueryParamValidatorTest {

  @Test
  @DisplayName("rejects values longer than the configured maximum")
  void requireMaxLength_exceeded_throwsBadRequest() {
    String tooLong = "x".repeat(200);
    assertThatThrownBy(() -> QueryParamValidator.requireMaxLength(tooLong, "city", 128))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("city");
  }

  @Test
  @DisplayName("accepts null values for optional query parameters")
  void requireMaxLength_null_passes() {
    assertThatCode(() -> QueryParamValidator.requireMaxLength(null, "city", 128))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rejects sort strings with invalid characters")
  void requireValidSortParam_invalidChars_throwsBadRequest() {
    assertThatThrownBy(() -> QueryParamValidator.requireValidSortParam("energy;drop", 64))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("invalid format");
  }

  @Test
  @DisplayName("rejects sort strings longer than the configured maximum")
  void requireValidSortParam_exceededMaxLength_throwsBadRequest() {
    String tooLong = "a".repeat(65);
    assertThatThrownBy(() -> QueryParamValidator.requireValidSortParam(tooLong, 64))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exceeds maximum length");
  }

  @Test
  @DisplayName("accepts well-formed sort expressions")
  void requireValidSortParam_validSort_passes() {
    assertThatCode(() -> QueryParamValidator.requireValidSortParam("energy,desc", 64))
        .doesNotThrowAnyException();
  }
}
