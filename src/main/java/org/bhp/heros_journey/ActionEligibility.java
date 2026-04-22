package org.bhp.heros_journey;

/**
 * Represents whether a player action can be attempted based on their skills and game state.
 * This record captures the "eligibility phase" of action resolution.
 */
public record ActionEligibility(
        boolean canAttempt,
        String skillName,
        int skillInitialLevel,
        boolean isExistingSkill,
        String description
) {
}
