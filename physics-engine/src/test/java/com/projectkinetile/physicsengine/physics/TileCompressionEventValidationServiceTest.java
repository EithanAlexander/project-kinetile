package com.projectkinetile.physicsengine.physics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.projectkinetile.physicsengine.domain.TileCompressionEventType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@DisplayName("Tile compression event validation")
class TileCompressionEventValidationServiceTest {

  private static final UUID TILE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

  private TileCompressionEventValidationService validationService;

  @BeforeEach
  void setUp() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    validationService = new TileCompressionEventValidationService(validator);
  }

  @Test
  @DisplayName("valid event produces no violations")
  void validate_validEvent_noViolations() {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-1",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            80.0,
            1.2,
            Instant.parse("2024-01-01T00:00:00Z"));
    assertThat(validationService.validate(event)).isEmpty();
  }

  @Test
  @DisplayName("impact multiplier outside range 1.0–1.5 reports violation")
  void validate_impactMultiplierOutOfRange_reportsViolation() {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-1",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            80.0,
            2.0,
            Instant.parse("2024-01-01T00:00:00Z"));
    List<String> violations = validationService.validate(event);
    assertThat(violations).anyMatch(v -> v.startsWith("impactMultiplier:"));
  }

  @Test
  @DisplayName("null required fields report violations")
  void validate_nullRequiredFields_reportsViolations() {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-1",
            null,
            null,
            80.0,
            1.2,
            null);
    List<String> violations = validationService.validate(event);
    assertThat(violations)
        .anyMatch(v -> v.startsWith("eventType:"))
        .anyMatch(v -> v.startsWith("tileId:"))
        .anyMatch(v -> v.startsWith("timestamp:"));
  }

  @Test
  @DisplayName("multiple invalid fields report all violations")
  void validate_multipleInvalidFields_reportsAllViolations() {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "  ",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            -5.0,
            2.0,
            Instant.parse("2099-01-01T00:00:00Z"));
    List<String> violations = validationService.validate(event);
    assertThat(violations)
        .hasSizeGreaterThanOrEqualTo(4)
        .anyMatch(v -> v.startsWith("eventId:"))
        .anyMatch(v -> v.startsWith("massKg:"))
        .anyMatch(v -> v.startsWith("impactMultiplier:"))
        .anyMatch(v -> v.startsWith("timestamp:"));
  }
}
