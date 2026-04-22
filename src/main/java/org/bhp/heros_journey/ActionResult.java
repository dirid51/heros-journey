package org.bhp.heros_journey;

/**
 * Represents the outcome of an attempted action in terms of state changes.
 * This record is only populated if the action was successfully attempted.
 * All fields represent the cumulative effects of the action on the player.
 */
public record ActionResult(
        int xpGained,
        boolean success,
        boolean levelIncreased,
        int newLevel,
        int damageTaken,
        int healthBoost,
        int maxHealthIncrease,
        double injuryReductionGain,
        String description
) {
}
