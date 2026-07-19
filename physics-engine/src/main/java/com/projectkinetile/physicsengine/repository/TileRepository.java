package com.projectkinetile.physicsengine.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectkinetile.physicsengine.domain.TileEntity;

/** Repository for physical tiles in the infrastructure catalog. */
public interface TileRepository extends JpaRepository<TileEntity, Long> {

  Optional<TileEntity> findByTileId(UUID tileId);

  @EntityGraph(
      attributePaths = {
        "chokepoint",
        "chokepoint.city",
        "chokepoint.placeType",
        "manufacturer"
      })
  Optional<TileEntity> findDetailedByTileId(UUID tileId);

  @EntityGraph(attributePaths = {"manufacturer"})
  Page<TileEntity> findByChokepointIdOrderByTileIdAsc(Long chokepointId, Pageable pageable);

  @Query(
      "SELECT COUNT(t) FROM TileEntity t WHERE t.active = true AND t.chokepoint.city.id = :cityId")
  long countActiveByCityId(@Param("cityId") Long cityId);

  @Query(
      "SELECT COUNT(t) FROM TileEntity t WHERE t.active = true AND t.chokepoint.id = :chokepointId")
  long countActiveByChokepointId(@Param("chokepointId") Long chokepointId);

  @Query(
      "SELECT COUNT(t) FROM TileEntity t WHERE t.active = true AND t.manufacturer.id = :manufacturerId")
  long countActiveByManufacturerId(@Param("manufacturerId") Long manufacturerId);

  /** Returns active tile counts grouped by city id: {@code [cityId, count]}. */
  @Query(
      "SELECT t.chokepoint.city.id, COUNT(t) FROM TileEntity t WHERE t.active = true GROUP BY t.chokepoint.city.id")
  List<Object[]> countActiveTilesByCityIdGrouped();

  @EntityGraph(
      attributePaths = {
        "chokepoint",
        "chokepoint.city",
        "chokepoint.placeType",
        "manufacturer"
      })
  @Query(
      """
      SELECT t FROM TileEntity t
      LEFT JOIN TileCompressionActivityEntity a ON a.tileId = t.tileId
      WHERE t.active = true
      AND (a.lastCompressionAt IS NULL OR a.lastCompressionAt < :threshold)
      ORDER BY t.tileId
      """)
  List<TileEntity> findStaleActiveTiles(@Param("threshold") Instant threshold);
}
