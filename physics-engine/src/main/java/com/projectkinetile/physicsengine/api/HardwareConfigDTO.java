package com.projectkinetile.physicsengine.api;

import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;

/**
 *
 * Hardware tile configuration exposed to dashboard clients for calculator and
 * About views.
 *
 * @param activationThresholdNewtons minimum force (N) to trigger rated output
 * @param minRatedOutputJoules floor joules at minimum activating load
 * @param maxRatedOutputJoules ceiling joules at max scale mass (5 W·s)
 * @param maxScaleMassKg effective-mass axis (kg) where max joules is reached
 * @param maxDisplacementMeters nominal max displacement at threshold (m)
 * @param gravity standard gravity used in force calculation (m/s²)
 *
 */
public record HardwareConfigDTO(
        double activationThresholdNewtons,
        double minRatedOutputJoules,
        double maxRatedOutputJoules,
        double maxScaleMassKg,
        double maxDisplacementMeters,
        double gravity) {

    public static HardwareConfigDTO from(
            HardwareProperties hardwareProperties, PhysicsProperties physicsProperties) {
        return new HardwareConfigDTO(
                hardwareProperties.getActivationThresholdNewtons(),
                hardwareProperties.getMinRatedOutputJoules(),
                hardwareProperties.getMaxRatedOutputJoules(),
                hardwareProperties.getMaxScaleMassKg(),
                hardwareProperties.getMaxDisplacementMeters(),
                physicsProperties.getGravity());
    }
}
