package com.projectkinetile.physicsengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Location-kind registry row used to classify chokepoints and derive traffic tiers.
 *
 * <p>Examples: {@code MARKET}, {@code BUS_STATION}, {@code BIKE_LANE}.
 */
@Entity
@Table(name = "place_types")
public class PlaceTypeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String label;

  @Column(name = "traffic_tier", nullable = false, length = 16)
  private String trafficTier;

  /** Required by JPA. */
  public PlaceTypeEntity() {}

  public PlaceTypeEntity(String code, String label, String trafficTier) {
    this.code = code;
    this.label = label;
    this.trafficTier = trafficTier;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getTrafficTier() {
    return trafficTier;
  }

  public void setTrafficTier(String trafficTier) {
    this.trafficTier = trafficTier;
  }
}
