package com.projectkinetile.dbinit.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/** One physical tile instance in infrastructure-registry.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryTileInstance(
    String tileId,
    String manufacturer,
    String size,
    String color,
    LocalDate installationDate,
    LocalDate lastInspectionDate,
    boolean isActive) {}
