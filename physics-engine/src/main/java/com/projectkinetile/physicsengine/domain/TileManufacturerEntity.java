package com.projectkinetile.physicsengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tile vendor registry row.
 *
 * <p>Seeded manufacturers (fictional vendors — lore is docs-only, not stored in DB):
 *
 * <ul>
 *   <li><strong>Aslan</strong> — my beloved cat
 *   <li><strong>Æ Inc</strong> — house sigil
 *   <li><strong>GFS</strong> — Greenfields Shraga
 * </ul>
 *
 * <p>API and catalog responses expose {@code name} only.
 */
@Entity
@Table(name = "tile_manufacturers")
public class TileManufacturerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String name;

  /** Required by JPA. */
  public TileManufacturerEntity() {}

  public TileManufacturerEntity(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
