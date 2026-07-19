package com.projectkinetile.physicsengine.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.projectkinetile.physicsengine.config.HardwareProperties;
import com.projectkinetile.physicsengine.config.PhysicsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for threshold-activation {@link PiezoelectricCalculator} with weight-scaled J band.
 *
 * <p>{@code impactMultiplier} literals (e.g. 1.0, 1.2, 1.5) model real step intensity as a multiple
 * of body weight in the walking range 1.0–1.5× (Kashem et al.). They are supplied inputs to
 * {@link PiezoelectricCalculator#evaluate(double, double)}, not values this test invents for
 * convenience—the engine does not derive multiplier from speed. In production, multiplier arrives
 * with each compression event; on the dashboard it may be entered directly or suggested from
 * walking speed before the shared harvest formula runs.
 */
@DisplayName("Piezoelectric calculator")
class PiezoelectricCalculatorTest {

  private static PiezoelectricCalculator calculator() {
    PhysicsProperties physics = new PhysicsProperties();
    physics.setGravity(9.81);
    HardwareProperties hardware = new HardwareProperties();
    hardware.setActivationThresholdNewtons(100.0);
    hardware.setMinRatedOutputJoules(2.0);
    hardware.setMaxRatedOutputJoules(5.0);
    hardware.setMaxScaleMassKg(90.0);
    return new PiezoelectricCalculator(physics, hardware);
  }

  private final PiezoelectricCalculator calculator = calculator();

  @Test
  @DisplayName("typical male walker (80 kg, 1.0×) activates with scaled J")
  void evaluate_typicalMaleWalker_activates() {
    ActivationResult result = calculator.evaluate(80.0, 1.0);
    assertThat(result.forceNewtons()).isCloseTo(784.8, within(0.1));
    assertThat(result.activationSuccessful()).isTrue();
    assertThat(result.energyJoules()).isCloseTo(4.63, within(0.02));
  }

  @Test
  @DisplayName("typical female with load (65 kg, 1.2×) activates with scaled J")
  void evaluate_typicalFemaleWithLoad_activates() {
    ActivationResult result = calculator.evaluate(65.0, 1.2);
    assertThat(result.forceNewtons()).isCloseTo(765.18, within(0.1));
    assertThat(result.activationSuccessful()).isTrue();
    assertThat(result.energyJoules()).isCloseTo(4.56, within(0.02));
  }

  @Test
  @DisplayName("8 kg below 100 N threshold returns 0 J")
  void evaluate_lightMass_doesNotActivate() {
    ActivationResult result = calculator.evaluate(8.0, 1.0);
    assertThat(result.forceNewtons()).isCloseTo(78.48, within(0.01));
    assertThat(result.activationSuccessful()).isFalse();
    assertThat(result.energyJoules()).isZero();
  }

  @Test
  @DisplayName("at-threshold mass (~10.2 kg) activates at floor J")
  void evaluate_atThreshold_activatesAtFloor() {
    ActivationResult result = calculator.evaluate(10.2, 1.0);
    assertThat(result.forceNewtons()).isGreaterThanOrEqualTo(100.0);
    assertThat(result.activationSuccessful()).isTrue();
    assertThat(result.energyJoules()).isCloseTo(2.0, within(0.05));
  }

  @Test
  @DisplayName("heavy walker (95 kg) caps at max rated J")
  void evaluate_heavyWalker_capsAtMax() {
    ActivationResult result = calculator.evaluate(95.0, 1.0);
    assertThat(result.activationSuccessful()).isTrue();
    assertThat(result.energyJoules()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("hard step (80 kg, 1.5×) reaches max J via effective load cap")
  void evaluate_hardStep_reachesMaxJoules() {
    ActivationResult result = calculator.evaluate(80.0, 1.5);
    assertThat(result.activationSuccessful()).isTrue();
    assertThat(result.energyJoules()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("returns zero for invalid mass or impact multiplier")
  void evaluate_invalidInputs_returnsZero() {
    assertThat(calculator.evaluate(0.0, 1.0).energyJoules()).isZero();
    assertThat(calculator.evaluate(-1.0, 1.0).energyJoules()).isZero();
    assertThat(calculator.evaluate(70.0, 0.9).energyJoules()).isZero();
    assertThat(calculator.evaluate(70.0, 1.6).energyJoules()).isZero();
  }

  @Test
  @DisplayName("paper weekly corridor aggregation sanity check (80 kg walker, scaled J)")
  void weeklyCorridorAggregation_sanityCheck() {
    long steps = 2_970_000L;
    double joulesPerStep = calculator.evaluate(80.0, 1.0).energyJoules();
    double totalJoules = steps * joulesPerStep;
    double wh = totalJoules / 3600.0;
    // Weight-scaled band: 80 kg at 1.0× yields ~4.63 J/step, so 2.97M steps ≈ 3,815 Wh.
    assertThat(wh).isCloseTo(3814.85, within(1.0));
  }
}
