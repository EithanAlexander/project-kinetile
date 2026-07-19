package com.projectkinetile.physicsengine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;
import com.projectkinetile.physicsengine.repository.TileCompressionActivityRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(TileCompressionActivityIncrementer.class)
@ActiveProfiles("test")
@DisplayName("Tile compression activity incrementer")
class TileCompressionActivityIncrementerTest {

  private static final Instant T0 = Instant.parse("2024-06-01T12:00:00Z");
  private static final Instant T1 = Instant.parse("2024-06-02T12:00:00Z");

  private static @NonNull UUID tileId() {
    return Objects.requireNonNull(UUID.fromString("11111111-1111-4111-8111-111111111111"));
  }

  @Autowired private TileCompressionActivityIncrementer incrementer;
  @Autowired private TileCompressionActivityRepository repository;

  @Test
  @DisplayName("increment inserts then atomically increments")
  @Transactional
  void increment_insertsAndIncrements() {
    UUID tileId = tileId();
    incrementer.increment(tileId, T0);
    incrementer.increment(tileId, T1);

    TileCompressionActivityEntity activity = repository.findById(tileId).orElseThrow();
    assertThat(activity.getTotalCompressions()).isEqualTo(2L);
    assertThat(activity.getFirstCompressionAt()).isEqualTo(T0);
    assertThat(activity.getLastCompressionAt()).isEqualTo(T1);
  }

  @Test
  @DisplayName("increment keeps latest last_compression_at when events arrive out of order")
  @Transactional
  void increment_outOfOrderTimestamp_keepsGreatestLastCompressionAt() {
    UUID tileId = tileId();
    incrementer.increment(tileId, T1);
    incrementer.increment(tileId, T0);

    TileCompressionActivityEntity activity = repository.findById(tileId).orElseThrow();
    assertThat(activity.getTotalCompressions()).isEqualTo(2L);
    assertThat(activity.getFirstCompressionAt()).isEqualTo(T1);
    assertThat(activity.getLastCompressionAt()).isEqualTo(T1);
  }
}
