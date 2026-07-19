package com.projectkinetile.physicsengine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectkinetile.physicsengine.config.TileMonitoringProperties;

/** Exposes tile monitoring thresholds for dashboard inventory views. */
@RestController
@RequestMapping(ApiPaths.CONFIG)
public class TileMonitoringConfigController {

  private static final Logger log = LoggerFactory.getLogger(TileMonitoringConfigController.class);

  private final TileMonitoringProperties tileMonitoringProperties;

  public TileMonitoringConfigController(TileMonitoringProperties tileMonitoringProperties) {
    this.tileMonitoringProperties = tileMonitoringProperties;
  }

  @GetMapping("/tile-monitoring")
  public TileMonitoringConfigDTO getTileMonitoringConfig() {
    log.debug("Tile monitoring configuration returned for the frontend");
    return TileMonitoringConfigDTO.from(tileMonitoringProperties);
  }
}
