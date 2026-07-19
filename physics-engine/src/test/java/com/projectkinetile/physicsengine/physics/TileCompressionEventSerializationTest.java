package com.projectkinetile.physicsengine.physics;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;

@DisplayName("Tile compression event serialization")
class TileCompressionEventSerializationTest {

  private static final UUID TILE_ID = UUID.fromString("a3f8c2d1-9b4e-4f1a-8c7d-2e6f5a4b3c1d");

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
  }

  @Test
  @DisplayName("deserializes snake_case JSON")
  void readValue_snakeCaseJson() throws Exception {
    String json =
        """
        {
          "event_id": "evt-001",
          "event_type": "tile_compression",
          "tile_id": "a3f8c2d1-9b4e-4f1a-8c7d-2e6f5a4b3c1d",
          "mass_kg": 87.4,
          "impact_multiplier": 1.23,
          "timestamp": "2024-06-15T10:30:00Z"
        }
        """;
    TileCompressionEvent event = objectMapper.readValue(json, TileCompressionEvent.class);
    assertThat(event.eventId()).isEqualTo("evt-001");
    assertThat(event.eventType()).isEqualTo(TileCompressionEventType.TILE_COMPRESSION);
    assertThat(event.tileId()).isEqualTo(TILE_ID);
    assertThat(event.massKg()).isEqualTo(87.4);
    assertThat(event.impactMultiplier()).isEqualTo(1.23);
    assertThat(event.timestamp()).isEqualTo(Instant.parse("2024-06-15T10:30:00Z"));
  }

  @Test
  @DisplayName("round-trips through snake_case JSON without data loss")
  void writeThenReadValue_preservesEventAndSnakeCaseKeys() throws Exception {
    TileCompressionEvent original =
        new TileCompressionEvent(
            "evt-042",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            72.5,
            1.35,
            Instant.parse("2024-07-01T08:15:30Z"));

    String json = objectMapper.writeValueAsString(original);
    assertThat(json)
        .contains("\"event_id\"")
        .contains("\"event_type\":\"tile_compression\"")
        .contains("\"tile_id\"")
        .contains("\"mass_kg\"")
        .contains("\"impact_multiplier\"");

    TileCompressionEvent roundTripped = objectMapper.readValue(json, TileCompressionEvent.class);
    assertThat(roundTripped).isEqualTo(original);
  }

  @Test
  @DisplayName("rejects JSON containing unknown properties")
  void readValue_unknownProperty_throws() {
    String json =
        """
        {
          "event_id": "evt-002",
          "event_type": "tile_compression",
          "tile_id": "a3f8c2d1-9b4e-4f1a-8c7d-2e6f5a4b3c1d",
          "mass_kg": 65.0,
          "impact_multiplier": 1.10,
          "timestamp": "2024-06-15T10:30:00Z",
          "unexpected_field": "malicious payload"
        }
        """;

    assertThatThrownBy(() -> objectMapper.readValue(json, TileCompressionEvent.class))
        .isInstanceOf(UnrecognizedPropertyException.class);
  }
}
