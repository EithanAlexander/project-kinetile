package com.projectkinetile.physicsengine.api;

/** City catalog row with aggregate counts. */
public record CityCatalogDTO(Long id, String code, String name, long chokepointCount, long activeTileCount) {}
