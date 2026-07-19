package com.projectkinetile.physicsengine.kafka;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.physics.ActivationResult;
import com.projectkinetile.physicsengine.physics.PiezoelectricCalculator;
import com.projectkinetile.physicsengine.physics.TileCompressionEvent;
import com.projectkinetile.physicsengine.physics.TileCompressionEventValidationService;
import com.projectkinetile.physicsengine.service.TileCompressionPersistenceService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tile compression event consumer")
class TileCompressionEventConsumerTest {

  private static final UUID TILE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

  @Mock private ObjectMapper objectMapper;
  @Mock private PiezoelectricCalculator calculator;
  @Mock private TileCompressionEventValidationService validationService;
  @Mock private TileCompressionPersistenceService persistenceService;
  @Mock private TrafficEventDlqPublisher dlqPublisher;
  @Mock private TileEntity tileEntity;

  private TileCompressionEventConsumer consumer;

  @BeforeEach
  void setUp() {
    AppSecurityProperties props = new AppSecurityProperties();
    props.getKafka().getConsumer().setMaxPayloadBytes(8192);
    consumer =
        new TileCompressionEventConsumer(
            objectMapper,
            calculator,
            validationService,
            persistenceService,
            dlqPublisher,
            props);
  }

  @Test
  @DisplayName("persists compression entity for valid JSON")
  void consume_validJson_persistsEntity() throws Exception {
    Instant timestamp = Instant.parse("2024-03-01T08:00:00Z");
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-a",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            80.0,
            1.0,
            timestamp);
    String payload = "{\"ignored\":true}";
    ConsumerRecord<String, String> consumerRecord =
        new ConsumerRecord<>("raw-traffic-events", 0, 1L, "k", payload);

    when(objectMapper.readValue(payload, TileCompressionEvent.class)).thenReturn(event);
    when(validationService.validate(event)).thenReturn(List.of());
    when(persistenceService.resolveActiveTile(event)).thenReturn(Optional.of(tileEntity));
    when(calculator.evaluate(80.0, 1.0))
        .thenReturn(new ActivationResult(784.8, 4.624, true));

    TileCompressionEventEntity saved = new TileCompressionEventEntity();
    saved.setId(1L);
    AtomicInteger persists = new AtomicInteger();
    when(persistenceService.persist(
            eq(event), eq(tileEntity), ArgumentMatchers.any(ActivationResult.class)))
        .thenAnswer(
            inv -> {
              persists.incrementAndGet();
              return saved;
            });

    consumer.consume(consumerRecord);

    assertThat(persists.get()).isEqualTo(1);
    verify(dlqPublisher, never()).publish(eq("k"), eq(payload), ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("validation failure sends payload to DLQ")
  void consume_validationFailure_sendsToDlq() throws Exception {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-b",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            80.0,
            1.0,
            Instant.parse("2024-01-01T00:00:00Z"));
    String payload = "{}";
    ConsumerRecord<String, String> consumerRecord =
        new ConsumerRecord<>("raw-traffic-events", 0, 2L, "k", payload);

    when(objectMapper.readValue(payload, TileCompressionEvent.class)).thenReturn(event);
    when(validationService.validate(event))
        .thenReturn(List.of("impactMultiplier: must be less than or equal to 1.5"));

    consumer.consume(consumerRecord);

    verify(persistenceService, never()).persist(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    verify(dlqPublisher).publish("k", payload, "validation_failed");
  }

  @Test
  @DisplayName("oversized payload sends message to DLQ")
  void consume_oversizedPayload_sendsToDlq() {
    String payload = "x".repeat(9000);
    ConsumerRecord<String, String> consumerRecord =
        new ConsumerRecord<>("raw-traffic-events", 0, 3L, "k", payload);

    consumer.consume(consumerRecord);

    verify(persistenceService, never()).persist(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    verify(dlqPublisher).publish(eq("k"), eq(payload), org.mockito.ArgumentMatchers.contains("payload_too_large"));
  }

  @Test
  @DisplayName("unknown tile sends payload to DLQ")
  void consume_unknownTile_sendsToDlq() throws Exception {
    TileCompressionEvent event =
        new TileCompressionEvent(
            "evt-c",
            TileCompressionEventType.TILE_COMPRESSION,
            TILE_ID,
            80.0,
            1.0,
            Instant.parse("2024-01-01T00:00:00Z"));
    String payload = "{}";
    ConsumerRecord<String, String> consumerRecord =
        new ConsumerRecord<>("raw-traffic-events", 0, 4L, "k", payload);

    when(objectMapper.readValue(payload, TileCompressionEvent.class)).thenReturn(event);
    when(validationService.validate(event)).thenReturn(List.of());
    when(persistenceService.resolveActiveTile(event)).thenReturn(Optional.empty());

    consumer.consume(consumerRecord);

    verify(dlqPublisher).publish("k", payload, "unknown_or_inactive_tile");
  }
}
