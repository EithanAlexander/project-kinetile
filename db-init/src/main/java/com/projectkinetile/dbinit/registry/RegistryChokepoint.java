package com.projectkinetile.dbinit.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Chokepoint entry in infrastructure-registry.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryChokepoint(
    String code, String name, String placeType, List<RegistryTileInstance> tileInstances) {}
