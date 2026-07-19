package com.projectkinetile.physicsengine.api;

import java.time.Instant;
import java.util.UUID;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;

/** One ledger row with catalog labels resolved via tile joins. */
public record CompressionLedgerEntryDTO(
    Long id,
    String eventId,
    UUID tileId,
    String city,
    String location,
    String manufacturerName,
    double massKg,
    double impactMultiplier,
    double calculatedForceNewtons,
    double calculatedEnergyJoules,
    boolean activationSuccessful,
    Instant eventTimestamp) {

  private static final String UNKNOWN_LABEL = "Unknown";

  public static CompressionLedgerEntryDTO from(TileCompressionEventEntity entity) {
    var tile = entity.getTile();
    var chokepoint = tile != null ? tile.getChokepoint() : null;
    var city =
        chokepoint != null && chokepoint.getCity() != null
            ? chokepoint.getCity().getName()
            : UNKNOWN_LABEL;
    var location = chokepoint != null ? chokepoint.getName() : UNKNOWN_LABEL;
    var manufacturer =
        tile != null && tile.getManufacturer() != null
            ? tile.getManufacturer().getName()
            : UNKNOWN_LABEL;
    return new CompressionLedgerEntryDTO(
        entity.getId(),
        entity.getEventId(),
        entity.getTileId(),
        city,
        location,
        manufacturer,
        entity.getMassKg(),
        entity.getImpactMultiplier(),
        entity.getCalculatedForceNewtons(),
        entity.getCalculatedEnergyJoules(),
        entity.isActivationSuccessful(),
        entity.getEventTimestamp());
  }
}
