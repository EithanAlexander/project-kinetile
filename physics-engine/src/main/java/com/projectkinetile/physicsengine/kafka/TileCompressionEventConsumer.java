package com.projectkinetile.physicsengine.kafka;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.physics.ActivationResult;
import com.projectkinetile.physicsengine.physics.PiezoelectricCalculator;
import com.projectkinetile.physicsengine.physics.TileCompressionEvent;
import com.projectkinetile.physicsengine.physics.TileCompressionEventValidationService;
import com.projectkinetile.physicsengine.service.TileCompressionPersistenceService;

/** Consumes tile compression JSON from Kafka, validates payloads, and persists activation outcomes. */
@Component
public class TileCompressionEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(TileCompressionEventConsumer.class);

  private final ObjectMapper objectMapper;
  private final PiezoelectricCalculator calculator;
  private final TileCompressionEventValidationService validationService;
  private final TileCompressionPersistenceService persistenceService;
  private final TrafficEventDlqPublisher dlqPublisher;
  private final int maxPayloadBytes;

  public TileCompressionEventConsumer(
      ObjectMapper objectMapper,
      PiezoelectricCalculator calculator,
      TileCompressionEventValidationService validationService,
      TileCompressionPersistenceService persistenceService,
      TrafficEventDlqPublisher dlqPublisher,
      AppSecurityProperties appSecurityProperties) {
    this.objectMapper = objectMapper;
    this.calculator = calculator;
    this.validationService = validationService;
    this.persistenceService = persistenceService;
    this.dlqPublisher = dlqPublisher;
    this.maxPayloadBytes = appSecurityProperties.getKafka().getConsumer().getMaxPayloadBytes();
  }

  @KafkaListener(
      topics = "${app.kafka.topics.raw-traffic}",
      groupId = "${app.kafka.consumer.group-id}")
  public void consume(ConsumerRecord<String, String> consumerRecord) {
    String value = consumerRecord.value();
    if (value == null || value.isBlank()) {
      log.warn("Skipping null / empty Kafka value at offset {}", consumerRecord.offset());
      return;
    }

    int byteLength = value.getBytes(StandardCharsets.UTF_8).length;
    if (byteLength > maxPayloadBytes) {
      log.warn(
          "Skipping oversized payload at offset {} ({} bytes, max {})",
          consumerRecord.offset(),
          byteLength,
          maxPayloadBytes);
      dlqPublisher.publish(consumerRecord.key(), value, "payload_too_large: " + byteLength);
      return;
    }

    try {
      TileCompressionEvent event = objectMapper.readValue(value, TileCompressionEvent.class);
      List<String> violations = validationService.validate(event);
      if (!violations.isEmpty()) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Invalid TileCompressionEvent at offset {} (digest={}): {}",
              consumerRecord.offset(),
              payloadDigest(value),
              violations);
        }
        dlqPublisher.publish(consumerRecord.key(), value, "validation_failed");
        return;
      }

      Optional<TileEntity> tileOpt = persistenceService.resolveActiveTile(event);
      if (tileOpt.isEmpty()) {
        log.warn(
            "Unknown or inactive tile {} at offset {}",
            event.tileId(),
            consumerRecord.offset());
        dlqPublisher.publish(consumerRecord.key(), value, "unknown_or_inactive_tile");
        return;
      }

      ActivationResult result = calculator.evaluate(event.massKg(), event.impactMultiplier());

      log.info(
          "Evaluated compression {} on tile {}: {} N, {} J, activated={} at {}",
          event.eventId(),
          event.tileId(),
          result.forceNewtons(),
          result.energyJoules(),
          result.activationSuccessful(),
          event.timestamp());

      TileCompressionEventEntity saved =
          persistenceService.persist(event, tileOpt.get(), result);

      log.info(
          "Persisted tile compression id={} eventId={}: {} J (activated={})",
          saved.getId(),
          event.eventId(),
          result.energyJoules(),
          result.activationSuccessful());

    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error(
            "Failed to deserialize TileCompressionEvent at offset {} (digest={}): {}",
            consumerRecord.offset(),
            payloadDigest(value),
            e.getOriginalMessage());
      }
      dlqPublisher.publish(consumerRecord.key(), value, "json_parse_error");
    }
  }

  private static String payloadDigest(String payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      return "len:" + payload.length();
    }
  }
}
