package org.bhp.heros_journey;

public record ActionOutcome(
        // Validation phase
        boolean canAttempt,
        String skillName,
        int skillInitialLevel,
        boolean isExistingSkill,

        // Result phase (only populated if canAttempt=true)
        int xpGained,
        boolean success,
        int damageTaken,
        int healthBoost,
        int maxHealthIncrease,
        double injuryReductionGain,

        // Inventory phase (null if no item interaction occurred)
        String itemPickedUp,  // item ID from the room's itemIds list (null if none picked up)
        String itemDropped,   // item ID from the player's inventory (null if none dropped)
        String itemUsed,      // item ID consumed from the player's inventory (null if none used)

        String description
) {
}