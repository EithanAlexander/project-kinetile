package com.projectkinetile.dbinit.registry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Root document for infrastructure-registry.json. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InfrastructureRegistry(
    List<String> manufacturers, List<RegistryCity> cities) {}
