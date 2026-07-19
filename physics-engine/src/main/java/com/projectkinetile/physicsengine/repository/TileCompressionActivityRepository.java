package com.projectkinetile.physicsengine.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectkinetile.physicsengine.domain.TileCompressionActivityEntity;

/** Repository for per-tile compression activity summaries. */
public interface TileCompressionActivityRepository
    extends JpaRepository<TileCompressionActivityEntity, UUID> {

  List<TileCompressionActivityEntity> findByTileIdIn(Collection<UUID> tileIds);
}
