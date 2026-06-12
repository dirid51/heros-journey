package org.bhp.heros_journey;

public record ActionOutcome(
        boolean canAttempt,
        String relevantSkillName,   // from skills.json, or null — replaces skillName/isExistingSkill/skillInitialLevel
        int spAwarded,              // replaces xpGained
        boolean success,
        int damageTaken,
        int healthBoost,
        int maxHealthIncrease,
        double injuryReductionGain,
        String itemPickedUp,
        String itemDropped,
        String itemUsed,
        String description
) {
}