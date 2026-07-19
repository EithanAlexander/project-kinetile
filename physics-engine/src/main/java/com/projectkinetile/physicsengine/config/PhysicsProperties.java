package com.projectkinetile.physicsengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global physics constants for force calculation, bound from {@code app.physics} in application YAML.
 */
@ConfigurationProperties(prefix = "app.physics")
public class PhysicsProperties {

  /** Standard gravity in m/s² ({@code Force = mass × gravity × impactMultiplier}). */
  private double gravity = 9.81;

  public double getGravity() {
    return gravity;
  }

  public void setGravity(double gravity) {
    this.gravity = gravity;
  }
}
