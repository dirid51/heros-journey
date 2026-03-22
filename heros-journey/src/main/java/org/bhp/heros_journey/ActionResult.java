package org.bhp.heros_journey;

public record ActionResult(boolean success,
                           boolean levelIncreased,
                           int newLevel,
                           int damageTaken,
                           int healthBoost,          // One-time +HP
                           int maxHealthIncrease,    // Permanent +Max HP
                           double injuryReductionGain, // Percentage reduction increase (e.g., 0.05)
                           String description) {
}
