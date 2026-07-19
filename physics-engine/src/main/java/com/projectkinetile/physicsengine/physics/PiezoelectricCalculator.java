package com.projectkinetile.physicsengine.physics;

import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evaluates tile compression events against the commercial threshold-activation model with a
 * weight-scaled rated output band: when downward force meets
 * {@code hardware.tile.activation-threshold-newtons}, joules scale linearly from
 * {@code min-rated-output-joules} at minimum activating load to {@code max-rated-output-joules} at
 * {@code max-scale-mass-kg} effective load; otherwise output is zero.
 */
@Service
public class PiezoelectricCalculator {

  private static final double MIN_IMPACT_MULTIPLIER = 1.0;
  private static final double MAX_IMPACT_MULTIPLIER = 1.5;

  private final Logger log = LoggerFactory.getLogger(PiezoelectricCalculator.class);

  private final PhysicsProperties physicsProperties;
  private final HardwareProperties hardwareProperties;

  public PiezoelectricCalculator(
      PhysicsProperties physicsProperties, HardwareProperties hardwareProperties) {
    this.physicsProperties = physicsProperties;
    this.hardwareProperties = hardwareProperties;
  }

  /**
   * Evaluates one compression event.
   *
   * @param massKg load mass in kilograms
   * @param impactMultiplier dynamic footfall multiplier (1.0–1.5× body weight)
   * @return force, energy, and activation flag
   */
  public ActivationResult evaluate(double massKg, double impactMultiplier) {
    if (massKg <= 0) {
      log.error("Invalid mass: must be positive: mass={}", massKg);
      return new ActivationResult(0.0, 0.0, false);
    }
    if (impactMultiplier < MIN_IMPACT_MULTIPLIER || impactMultiplier > MAX_IMPACT_MULTIPLIER) {
      log.error(
          "Invalid impactMultiplier: must be in [{}, {}]: impactMultiplier={}",
          MIN_IMPACT_MULTIPLIER,
          MAX_IMPACT_MULTIPLIER,
          impactMultiplier);
      return new ActivationResult(0.0, 0.0, false);
    }

    double force = massKg * physicsProperties.getGravity() * impactMultiplier;
    boolean activated = force >= hardwareProperties.getActivationThresholdNewtons();
    double joules = activated ? computeJoulesPerActivation(massKg, impactMultiplier) : 0.0;
    return new ActivationResult(force, joules, activated);
  }

  /**
   * Linearly scales joules between min and max rated output using effective load
   * {@code min(maxScaleMassKg, mass × impact)}.
   */
  private double computeJoulesPerActivation(double massKg, double impactMultiplier) {
    double gravity = physicsProperties.getGravity();
    double threshold = hardwareProperties.getActivationThresholdNewtons();
    double minJoules = hardwareProperties.getMinRatedOutputJoules();
    double maxJoules = hardwareProperties.getMaxRatedOutputJoules();
    double maxScaleMassKg = hardwareProperties.getMaxScaleMassKg();

    double minMass = threshold / (gravity * impactMultiplier);
    if (maxScaleMassKg <= minMass) {
      return minJoules;
    }

    double effectiveMass = Math.min(maxScaleMassKg, massKg * impactMultiplier);
    double span = maxScaleMassKg - minMass;
    double t = Math.clamp((effectiveMass - minMass) / span, 0.0, 1.0);
    double joules = minJoules + t * (maxJoules - minJoules);
    return Math.clamp(joules, minJoules, maxJoules);
  }
}
