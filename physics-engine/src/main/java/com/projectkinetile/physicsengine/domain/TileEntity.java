package com.projectkinetile.physicsengine.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Physical piezoelectric tile installed at a chokepoint. */
@Entity
@Table(name = "tiles")
public class TileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tile_id", nullable = false, unique = true)
  private UUID tileId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chokepoint_id", nullable = false)
  private ChokepointEntity chokepoint;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "manufacturer_id", nullable = false)
  private TileManufacturerEntity manufacturer;

  @Column(nullable = false, length = 32)
  private String size;

  @Column(nullable = false, length = 64)
  private String color;

  @Column(name = "installation_date", nullable = false)
  private LocalDate installationDate;

  @Column(name = "removal_date")
  private LocalDate removalDate;

  @Column(name = "last_inspection_date", nullable = false)
  private LocalDate lastInspectionDate;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  /** Required by JPA. */
  public TileEntity() {}

  public TileEntity(
      UUID tileId,
      ChokepointEntity chokepoint,
      TileManufacturerEntity manufacturer,
      String size,
      String color,
      LocalDate installationDate,
      LocalDate lastInspectionDate,
      boolean active) {
    this.tileId = tileId;
    this.chokepoint = chokepoint;
    this.manufacturer = manufacturer;
    this.size = size;
    this.color = color;
    this.installationDate = installationDate;
    this.lastInspectionDate = lastInspectionDate;
    this.active = active;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UUID getTileId() {
    return tileId;
  }

  public void setTileId(UUID tileId) {
    this.tileId = tileId;
  }

  public ChokepointEntity getChokepoint() {
    return chokepoint;
  }

  public void setChokepoint(ChokepointEntity chokepoint) {
    this.chokepoint = chokepoint;
  }

  public TileManufacturerEntity getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(TileManufacturerEntity manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public LocalDate getInstallationDate() {
    return installationDate;
  }

  public void setInstallationDate(LocalDate installationDate) {
    this.installationDate = installationDate;
  }

  public LocalDate getRemovalDate() {
    return removalDate;
  }

  public void setRemovalDate(LocalDate removalDate) {
    this.removalDate = removalDate;
  }

  public LocalDate getLastInspectionDate() {
    return lastInspectionDate;
  }

  public void setLastInspectionDate(LocalDate lastInspectionDate) {
    this.lastInspectionDate = lastInspectionDate;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
