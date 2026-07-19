package com.projectkinetile.physicsengine.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Rolling compression activity summary for one tile (1:1 with {@link TileEntity}). */
@Entity
@Table(name = "tile_compression_activity")
public class TileCompressionActivityEntity {

  @Id
  @Column(name = "tile_id")
  private UUID tileId;

  @Column(name = "first_compression_at", nullable = false, columnDefinition = "timestamp with time zone")
  private Instant firstCompressionAt;

  @Column(name = "last_compression_at", nullable = false, columnDefinition = "timestamp with time zone")
  private Instant lastCompressionAt;

  @Column(name = "total_compressions", nullable = false)
  private long totalCompressions;

  /** Required by JPA. */
  public TileCompressionActivityEntity() {}

  public TileCompressionActivityEntity(
      UUID tileId, Instant firstCompressionAt, Instant lastCompressionAt, long totalCompressions) {
    this.tileId = tileId;
    this.firstCompressionAt = firstCompressionAt;
    this.lastCompressionAt = lastCompressionAt;
    this.totalCompressions = totalCompressions;
  }

  public UUID getTileId() {
    return tileId;
  }

  public void setTileId(UUID tileId) {
    this.tileId = tileId;
  }

  public Instant getFirstCompressionAt() {
    return firstCompressionAt;
  }

  public void setFirstCompressionAt(Instant firstCompressionAt) {
    this.firstCompressionAt = firstCompressionAt;
  }

  public Instant getLastCompressionAt() {
    return lastCompressionAt;
  }

  public void setLastCompressionAt(Instant lastCompressionAt) {
    this.lastCompressionAt = lastCompressionAt;
  }

  public long getTotalCompressions() {
    return totalCompressions;
  }

  public void setTotalCompressions(long totalCompressions) {
    this.totalCompressions = totalCompressions;
  }
}
