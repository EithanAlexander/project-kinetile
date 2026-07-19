package com.projectkinetile.physicsengine.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.projectkinetile.physicsengine.domain.ChokepointEntity;
import com.projectkinetile.physicsengine.domain.CityEntity;
import com.projectkinetile.physicsengine.domain.PlaceTypeEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.domain.TileCompressionEventType;
import com.projectkinetile.physicsengine.domain.TileEntity;
import com.projectkinetile.physicsengine.domain.TileManufacturerEntity;
import com.projectkinetile.physicsengine.repository.ChokepointRepository;
import com.projectkinetile.physicsengine.repository.CityRepository;
import com.projectkinetile.physicsengine.repository.PlaceTypeRepository;
import com.projectkinetile.physicsengine.repository.TileManufacturerRepository;
import com.projectkinetile.physicsengine.repository.TileRepository;

import org.springframework.lang.NonNull;

/** Builds a minimal catalog graph for integration tests. */
public final class CatalogTestFixtures {

  private CatalogTestFixtures() {}

  public record CatalogSeed(
      @NonNull TileEntity tileA, @NonNull TileEntity tileB, @NonNull TileEntity tileC) {}

  public static @NonNull CatalogSeed seedCatalog(
      PlaceTypeRepository placeTypeRepository,
      TileManufacturerRepository manufacturerRepository,
      CityRepository cityRepository,
      ChokepointRepository chokepointRepository,
      TileRepository tileRepository) {
    PlaceTypeEntity market = placeTypeRepository.save(new PlaceTypeEntity("MARKET", "Market", "HIGH"));
    PlaceTypeEntity street = placeTypeRepository.save(new PlaceTypeEntity("STREET", "Street", "LOW"));
    TileManufacturerEntity aslan = manufacturerRepository.save(new TileManufacturerEntity("Aslan"));

    CityEntity telAviv = cityRepository.save(new CityEntity("TLV", "Tel Aviv-Yafo"));
    CityEntity haifa = cityRepository.save(new CityEntity("HFA", "Haifa"));

    ChokepointEntity siteA =
        chokepointRepository.save(new ChokepointEntity(telAviv, market, "SAA", "Site A"));
    ChokepointEntity siteB =
        chokepointRepository.save(new ChokepointEntity(haifa, street, "SBB", "Site B"));
    ChokepointEntity siteC =
        chokepointRepository.save(new ChokepointEntity(telAviv, market, "SCC", "Site C"));

    LocalDate installed = LocalDate.now().minusMonths(15);
    TileEntity tileA =
        Objects.requireNonNull(
            tileRepository.save(
                new TileEntity(
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    siteA,
                    aslan,
                    "600x600",
                    "Slate Gray",
                    installed,
                    installed,
                    true)));
    TileEntity tileB =
        Objects.requireNonNull(
            tileRepository.save(
                new TileEntity(
                    UUID.fromString("22222222-2222-4222-8222-222222222222"),
                    siteB,
                    aslan,
                    "600x600",
                    "Safety Yellow",
                    installed,
                    installed,
                    true)));
    TileEntity tileC =
        Objects.requireNonNull(
            tileRepository.save(
                new TileEntity(
                    UUID.fromString("33333333-3333-4333-8333-333333333333"),
                    siteC,
                    aslan,
                    "800x800",
                    "Slate Gray",
                    installed,
                    installed,
                    true)));
    return new CatalogSeed(tileA, tileB, tileC);
  }

  public static @NonNull TileCompressionEventEntity compressionEvent(
      String eventId,
      @NonNull TileEntity tile,
      double mass,
      double impact,
      double force,
      double joules,
      boolean activated,
      Instant ts,
      Instant audit) {
    return new TileCompressionEventEntity(
        eventId,
        TileCompressionEventType.TILE_COMPRESSION,
        tile,
        mass,
        impact,
        force,
        joules,
        activated,
        ts,
        audit,
        audit);
  }
}
