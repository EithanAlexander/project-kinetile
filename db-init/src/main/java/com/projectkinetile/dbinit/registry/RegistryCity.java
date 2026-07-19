package com.projectkinetile.dbinit.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** City entry in infrastructure-registry.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryCity(String code, String name, List<RegistryChokepoint> chokepoints) {}
