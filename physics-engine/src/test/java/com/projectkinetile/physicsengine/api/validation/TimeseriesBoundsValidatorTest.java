package com.projectkinetile.physicsengine.api.validation;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Timeseries bounds validator")
class TimeseriesBoundsValidatorTest {

  @Test
  @DisplayName("applies default window when since and until are omitted")
  void resolve_omittedBounds_appliesDefaultWindow() {
    Instant until = Instant.parse("2024-06-01T12:00:00Z");

    var bounds = TimeseriesBoundsValidator.resolve(null, until, 12);

    assertThat(bounds.until()).isEqualTo(until);
    assertThat(bounds.since()).isBefore(until);
  }

  @Test
  @DisplayName("rejects inverted time range")
  void resolve_invertedRange_throwsBadRequest() {
    Instant since = Instant.parse("2024-06-02T00:00:00Z");
    Instant until = Instant.parse("2024-06-01T00:00:00Z");

    assertThatThrownBy(() -> TimeseriesBoundsValidator.resolve(since, until, 12))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("time range is invalid");
  }

  @Test
  @DisplayName("rejects span wider than configured maximum months")
  void resolve_spanExceedsMaxMonths_throwsBadRequest() {
    Instant since = Instant.parse("2020-01-01T00:00:00Z");
    Instant until = Instant.parse("2024-06-01T00:00:00Z");

    assertThatThrownBy(() -> TimeseriesBoundsValidator.resolve(since, until, 6))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exceeds maximum");
  }
}
