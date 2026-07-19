package com.projectkinetile.physicsengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectkinetile.physicsengine.domain.PlaceTypeEntity;

/** Repository for place type lookup rows. */
public interface PlaceTypeRepository extends JpaRepository<PlaceTypeEntity, Long> {}
