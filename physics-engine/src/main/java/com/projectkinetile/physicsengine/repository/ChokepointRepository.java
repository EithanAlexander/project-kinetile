package com.projectkinetile.physicsengine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.projectkinetile.physicsengine.domain.ChokepointEntity;

/** Repository for chokepoint registry rows. */
public interface ChokepointRepository extends JpaRepository<ChokepointEntity, Long> {

  @EntityGraph(attributePaths = {"placeType", "city"})
  List<ChokepointEntity> findByCityIdOrderByNameAsc(Long cityId);

  /** Returns chokepoint counts grouped by city id: {@code [cityId, count]}. */
  @Query("SELECT cp.city.id, COUNT(cp) FROM ChokepointEntity cp GROUP BY cp.city.id")
  List<Object[]> countChokepointsByCityIdGrouped();
}
