package com.projectkinetile.physicsengine.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectkinetile.physicsengine.service.EdgeDeviceCatalogService;

/**
 * Exposes the configured edge-device catalog at {@code GET} {@link ApiPaths#DEVICES} (not under
 * {@link ApiPaths#ENERGY}).
 */
@RestController
public class EdgeDeviceCatalogController {

  private final EdgeDeviceCatalogService edgeDeviceCatalogService;

  public EdgeDeviceCatalogController(EdgeDeviceCatalogService edgeDeviceCatalogService) {
    this.edgeDeviceCatalogService = edgeDeviceCatalogService;
  }

  @GetMapping(ApiPaths.DEVICES)
  public List<EdgeDeviceDTO> getEdgeDeviceCatalog() {
    return edgeDeviceCatalogService.getCatalog();
  }
}
