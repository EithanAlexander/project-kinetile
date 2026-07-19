package com.projectkinetile.physicsengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Commercial piezoelectric tile hardware constants (threshold activation + weight-scaled rated
 * output band), bound from {@code hardware.tile} in application YAML.
 */
@ConfigurationProperties(prefix = "hardware.tile")
public class HardwareProperties {

  /** Minimum downward force (N) required to trigger rated electrical output. */
  private double activationThresholdNewtons = 100.0;

  /** Minimum electrical output (J) at the lightest activating load. */
  private double minRatedOutputJoules = 2.0;

  /** Maximum electrical output (J) at {@link #maxScaleMassKg} effective load and above. */
  private double maxRatedOutputJoules = 5.0;

  /** Effective-mass axis (kg) where {@link #maxRatedOutputJoules} is reached. */
  private double maxScaleMassKg = 90.0;

  /** Maximum mechanical displacement at threshold (m); display/metadata only. */
  private double maxDisplacementMeters = 0.0001;

  public double getActivationThresholdNewtons() {
    return activationThresholdNewtons;
  }

  public void setActivationThresholdNewtons(double activationThresholdNewtons) {
    this.activationThresholdNewtons = activationThresholdNewtons;
  }

  public double getMinRatedOutputJoules() {
    return minRatedOutputJoules;
  }

  public void setMinRatedOutputJoules(double minRatedOutputJoules) {
    this.minRatedOutputJoules = minRatedOutputJoules;
  }

  public double getMaxRatedOutputJoules() {
    return maxRatedOutputJoules;
  }

  public void setMaxRatedOutputJoules(double maxRatedOutputJoules) {
    this.maxRatedOutputJoules = maxRatedOutputJoules;
  }

  public double getMaxScaleMassKg() {
    return maxScaleMassKg;
  }

  public void setMaxScaleMassKg(double maxScaleMassKg) {
    this.maxScaleMassKg = maxScaleMassKg;
  }

  public double getMaxDisplacementMeters() {
    return maxDisplacementMeters;
  }

  public void setMaxDisplacementMeters(double maxDisplacementMeters) {
    this.maxDisplacementMeters = maxDisplacementMeters;
  }
}
