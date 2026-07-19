package com.projectkinetile.physicsengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

/**
 * Classpath location of the edge-device catalog JSON, bound from {@code app.edge-devices} in
 * application YAML.
 */
@ConfigurationProperties(prefix = "app.edge-devices")
public class EdgeDeviceCatalogProperties {

  private static final String DEFAULT_CATALOG_RESOURCE = "data/edge-devices.json";

  /**
   * Classpath-relative path to the catalog file (no leading slash), e.g. {@code
   * data/edge-devices.json}. Override via {@code app.edge-devices.catalog-resource} or env {@code
   * APP_EDGE_DEVICES_CATALOG_RESOURCE}.
   */
  private String catalogResource;

  @NonNull
  public String getCatalogResource() {
    return catalogResource != null ? catalogResource : DEFAULT_CATALOG_RESOURCE;
  }

  public void setCatalogResource(String catalogResource) {
    this.catalogResource = catalogResource;
  }
}
