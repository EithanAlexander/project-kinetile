package com.projectkinetile.physicsengine.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkinetile.physicsengine.api.EdgeDeviceDTO;
import com.projectkinetile.physicsengine.config.EdgeDeviceCatalogProperties;

/**
 * Loads the edge-device catalog once from a classpath JSON resource configured under {@code
 * app.edge-devices.catalog-resource} (default {@code data/edge-devices.json}). Edit that file or
 * point the property at another classpath resource and restart to change catalog entries.
 *
 * <p>If the resource is missing, unreadable, or not valid JSON for a device list, this constructor
 * throws {@link IllegalStateException} and Spring fails to start the application context — the
 * catalog is required.</p>
 */
@Service
public class EdgeDeviceCatalogService {

  private final List<EdgeDeviceDTO> catalog;

  public EdgeDeviceCatalogService(
      ObjectMapper objectMapper, EdgeDeviceCatalogProperties catalogProperties) {
    String catalogResource =
        Objects.requireNonNull(
            catalogProperties.getCatalogResource(),
            "device catalog must not be null");
    ClassPathResource resource = new ClassPathResource(catalogResource);
    try (InputStream in = resource.getInputStream()) {
      List<EdgeDeviceDTO> parsed =
          objectMapper.readValue(in, new TypeReference<List<EdgeDeviceDTO>>() {});
      this.catalog = List.copyOf(parsed);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Required edge device catalog could not be loaded (classpath:" + catalogResource + ")", e);
    }
  }

  /** Immutable snapshot of devices parsed at startup. */
  public List<EdgeDeviceDTO> getCatalog() {
    return catalog;
  }
}
