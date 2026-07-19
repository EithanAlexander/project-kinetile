package com.projectkinetile.physicsengine.physics;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Validates {@link TileCompressionEvent} payloads using Jakarta Bean Validation before persistence.
 */
@Service
public class TileCompressionEventValidationService {

  private final Validator validator;

  public TileCompressionEventValidationService(Validator validator) {
    this.validator = validator;
  }

  /**
   * @param event deserialized Kafka payload
   * @return human-readable violation messages; empty when valid
   */
  public List<String> validate(TileCompressionEvent event) {
    return validator.validate(event).stream()
        .map(TileCompressionEventValidationService::formatViolation)
        .toList();
  }

  private static String formatViolation(ConstraintViolation<TileCompressionEvent> violation) {
    return violation.getPropertyPath() + ": " + violation.getMessage();
  }
}
