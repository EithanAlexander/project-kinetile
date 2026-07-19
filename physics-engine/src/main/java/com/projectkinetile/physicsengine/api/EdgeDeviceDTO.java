package com.projectkinetile.physicsengine.api;

/**
 * Catalog entry for a target edge device and its typical daily energy requirement.
 *
 * @param id stable machine key for the device type
 * @param name human-readable label
 * @param dailyRequiredWh approximate daily energy need in watt-hours
 */
public record EdgeDeviceDTO(String id, String name, double dailyRequiredWh) {}
