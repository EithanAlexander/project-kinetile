package com.projectkinetile.physicsengine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.projectkinetile.physicsengine.domain.TileManufacturerEntity;

/** Repository for tile manufacturer registry rows. */
public interface TileManufacturerRepository extends JpaRepository<TileManufacturerEntity, Long> {

  /**
   * Returns each manufacturer with its active tile count in a single grouped query.
   *
   * @return rows of {@code [id, name, activeTileCount]}
   */
  @Query(
      """
      SELECT m.id, m.name, COUNT(t)
      FROM TileManufacturerEntity m
      LEFT JOIN TileEntity t ON t.manufacturer = m AND t.active = true
      GROUP BY m.id, m.name
      ORDER BY m.name
      """)
  List<Object[]> findAllWithActiveTileCounts();
}
