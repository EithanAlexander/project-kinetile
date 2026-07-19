package com.projectkinetile.physicsengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Pedestrian or light-mobility chokepoint within a city. */
@Entity
@Table(
    name = "chokepoints",
    uniqueConstraints = @UniqueConstraint(columnNames = {"city_id", "code"}))
public class ChokepointEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private CityEntity city;

  /** Many chokepoints share one place type; loaded only when accessed; FK is required. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_type_id", nullable = false)
  private PlaceTypeEntity placeType;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 256)
  private String name;

  /** Required by JPA. */
  public ChokepointEntity() {}

  public ChokepointEntity(CityEntity city, PlaceTypeEntity placeType, String code, String name) {
    this.city = city;
    this.placeType = placeType;
    this.code = code;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CityEntity getCity() {
    return city;
  }

  public void setCity(CityEntity city) {
    this.city = city;
  }

  public PlaceTypeEntity getPlaceType() {
    return placeType;
  }

  public void setPlaceType(PlaceTypeEntity placeType) {
    this.placeType = placeType;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
