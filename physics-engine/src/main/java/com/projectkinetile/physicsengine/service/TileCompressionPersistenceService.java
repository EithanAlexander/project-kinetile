package com.projectkinetile.physicsengine.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.physics.ActivationResult;
import com.projectkinetile.physicsengine.physics.TileCompressionEvent;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import com.projectkinetile.physicsengine.repository.TileRepository;

/** Validates tile identity and persists compression events with activity upserts. */
@Service
public class TileCompressionPersistenceService {

  private static final Logger log = LoggerFactory.getLogger(TileCompressionPersistenceService.class);

  private final TileRepository tileRepository;
  private final TileCompressionEventRepository eventRepository;
  private final TileCompressionActivityIncrementer activityIncrementer;

  public TileCompressionPersistenceService(
      TileRepository tileRepository,
      TileCompressionEventRepository eventRepository,
      TileCompressionActivityIncrementer activityIncrementer) {
    this.tileRepository = tileRepository;
    this.eventRepository = eventRepository;
    this.activityIncrementer = activityIncrementer;
  }

  /**
   * Resolves the tile for an event and validates lifecycle constraints.
   *
   * @return empty when the tile is missing, inactive, or the timestamp is outside install/removal
   */
  public Optional<TileEntity> resolveActiveTile(TileCompressionEvent event) {
    Optional<TileEntity> tileOpt = tileRepository.findByTileId(event.tileId());
    if (tileOpt.isEmpty()) {
      return Optional.empty();
    }
    TileEntity tile = tileOpt.get();
    if (!tile.isActive()) {
      return Optional.empty();
    }
    LocalDate eventDate = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
    if (eventDate.isBefore(tile.getInstallationDate())) {
      return Optional.empty();
    }
    if (tile.getRemovalDate() != null && eventDate.isAfter(tile.getRemovalDate())) {
      return Optional.empty();
    }
    return Optional.of(tile);
  }

  /**
   * Persists one evaluated compression event and upserts tile activity counters.
   *
   * <p>Idempotent on {@code event_id}: Kafka redeliveries return the existing row without double-
   * counting activity.
   */
  @Transactional
  public TileCompressionEventEntity persist(
      TileCompressionEvent event, TileEntity tile, ActivationResult result) {
    Optional<TileCompressionEventEntity> existing = eventRepository.findByEventId(event.eventId());
    if (existing.isPresent()) {
      log.debug("Skipping duplicate event_id={}", event.eventId());
      return existing.get();
    }

    Instant persistedAt = Instant.now();
    TileCompressionEventEntity entity =
        new TileCompressionEventEntity(
            event.eventId(),
            event.eventType(),
            tile,
            event.massKg(),
            event.impactMultiplier(),
            result.forceNewtons(),
            result.energyJoules(),
            result.activationSuccessful(),
            event.timestamp(),
            persistedAt,
            persistedAt);
    try {
      TileCompressionEventEntity saved = eventRepository.save(entity);
      upsertActivity(Objects.requireNonNull(tile.getTileId()), event.timestamp());
      return saved;
    } catch (DataIntegrityViolationException ex) {
      if (isDuplicateEventId(ex)) {
        log.debug("Concurrent duplicate event_id={}, returning existing row", event.eventId());
        return eventRepository
            .findByEventId(event.eventId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Duplicate event_id constraint hit but row not found: " + event.eventId()));
      }
      throw ex;
    }
  }

  private void upsertActivity(@NonNull UUID tileId, Instant eventTimestamp) {
    activityIncrementer.increment(tileId, eventTimestamp);
  }

  private static boolean isDuplicateEventId(DataIntegrityViolationException ex) {
    Throwable cause = ex.getCause();
    while (cause != null) {
      if (cause instanceof ConstraintViolationException violation) {
        String constraint = violation.getConstraintName();
        if (constraint != null && constraint.toLowerCase().contains("event_id")) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }
}
