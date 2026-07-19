package com.projectkinetile.physicsengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectkinetile.physicsengine.domain.CityEntity;

/** Repository for city registry rows. */
public interface CityRepository extends JpaRepository<CityEntity, Long> {}
