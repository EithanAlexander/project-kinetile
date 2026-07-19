package com.projectkinetile.physicsengine.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Wire and persistence values for inbound Kafka compression events.
 */
public enum TileCompressionEventType {
  TILE_COMPRESSION("tile_compression");

  private final String wireValue;

  TileCompressionEventType(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String toWireValue() {
    return wireValue;
  }

  @JsonCreator
  public static TileCompressionEventType fromString(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("eventType must not be blank");
    }
    String normalized = value.trim().toLowerCase();
    for (TileCompressionEventType type : values()) {
      if (type.wireValue.equals(normalized)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unsupported eventType: " + value);
  }
}
