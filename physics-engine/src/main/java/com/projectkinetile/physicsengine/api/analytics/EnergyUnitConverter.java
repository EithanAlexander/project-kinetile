package com.projectkinetile.physicsengine.api.analytics;

/** Converts physics energy outputs into practical electrical units. */
public final class EnergyUnitConverter {

  public static final double JOULES_PER_WATT_HOUR = 3600.0;

  private EnergyUnitConverter() {}

  /**
   * Converts Joules to Watt-hours rounded to three decimal places.
   *
   * @param totalJoules energy in Joules
   * @return equivalent Watt-hours
   */
  public static double wattHoursFromJoules(double totalJoules) {
    double wh = totalJoules / JOULES_PER_WATT_HOUR;
    return Math.round(wh * 1000.0) / 1000.0;
  }
}
