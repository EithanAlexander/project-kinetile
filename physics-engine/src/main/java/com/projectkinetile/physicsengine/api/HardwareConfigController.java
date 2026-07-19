package com.projectkinetile.physicsengine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;

/** Exposes hardware tile configuration for dashboard calculator and About views. */
@RestController
@RequestMapping(ApiPaths.CONFIG)
public class HardwareConfigController {

  private static final Logger log = LoggerFactory.getLogger(HardwareConfigController.class);

  private final HardwareProperties hardwareProperties;
  private final PhysicsProperties physicsProperties;

  public HardwareConfigController(
      HardwareProperties hardwareProperties, PhysicsProperties physicsProperties) {
    this.hardwareProperties = hardwareProperties;
    this.physicsProperties = physicsProperties;
  }

  @GetMapping("/hardware")
  public HardwareConfigDTO getHardwareConfig() {
    log.debug("Hardware configuration returned for the frontend");
    return HardwareConfigDTO.from(hardwareProperties, physicsProperties);
  }
}
