package com.projectkinetile.physicsengine.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.projectkinetile.physicsengine.domain.ApiRateLimitClientEntity;

import jakarta.persistence.LockModeType;

/** Persistence for distributed per-IP API rate-limit state. */
public interface ApiRateLimitClientRepository extends JpaRepository<ApiRateLimitClientEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<ApiRateLimitClientEntity> findByClientIp(String clientIp);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query(
      """
      DELETE FROM ApiRateLimitClientEntity c
      WHERE c.permanentlyBlocked = false
      AND (c.penaltyUntil IS NULL OR c.penaltyUntil < :now)
      AND c.lastSeenAt < :cutoff
      """)
  int deleteIdleBenignClients(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
