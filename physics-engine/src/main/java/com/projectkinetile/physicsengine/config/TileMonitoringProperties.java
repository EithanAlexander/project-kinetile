package com.projectkinetile.physicsengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for tile inactivity monitoring thresholds. */
@ConfigurationProperties(prefix = "app.tile-monitoring")
public class TileMonitoringProperties {

  private int inactivityThresholdDays = 5;

  public int getInactivityThresholdDays() {
    return inactivityThresholdDays;
  }

  public void setInactivityThresholdDays(int inactivityThresholdDays) {
    this.inactivityThresholdDays = inactivityThresholdDays;
  }
}
