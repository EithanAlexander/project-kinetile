package com.projectkinetile.physicsengine.physics;

/**
 * Outcome of evaluating one tile compression against the hardware threshold model.
 *
 * @param forceNewtons calculated downward force ({@code mass × gravity × impactMultiplier})
 * @param energyJoules rated output when threshold met, otherwise {@code 0.0}
 * @param activationSuccessful {@code true} when {@code forceNewtons >= activationThreshold}
 */
public record ActivationResult(
    double forceNewtons, double energyJoules, boolean activationSuccessful) {}
