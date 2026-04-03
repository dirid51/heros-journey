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
        boolean levelIncreased,
        int newLevel,
        int damageTaken,
        int healthBoost,
        int maxHealthIncrease,
        double injuryReductionGain,

        String description
) {
}
