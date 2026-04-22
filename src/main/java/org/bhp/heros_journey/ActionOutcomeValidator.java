package org.bhp.heros_journey;

/**
 * Validator for ActionOutcome records to prevent AI model from silently corrupting game state.
 * Ensures that all required fields are present and have valid values before applying state changes.
 */
public class ActionOutcomeValidator {

    private static final int MAX_SKILL_LEVEL = 100;
    private static final int MAX_DAMAGE = 1000;
    private static final int MAX_HEALTH_BOOST = 500;
    private static final double MAX_INJURY_REDUCTION = 0.9;

    private ActionOutcomeValidator() {
        // Utility class
    }

    /**
     * Validates an ActionOutcome record for consistency and reasonable values.
     *
     * @param outcome the ActionOutcome to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validate(ActionOutcome outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("ActionOutcome cannot be null");
        }

        // Required fields that must always be present
        if (outcome.skillName() == null || outcome.skillName().isBlank()) {
            throw new IllegalArgumentException("ActionOutcome.skillName must not be empty");
        }

        if (outcome.description() == null || outcome.description().isBlank()) {
            throw new IllegalArgumentException("ActionOutcome.description must not be empty");
        }

        // Validate canAttempt consistency
        if (!outcome.canAttempt() && outcome.skillInitialLevel() > 0) {
            throw new IllegalArgumentException(
                    "If canAttempt is false, skillInitialLevel must be 0, got: " + outcome.skillInitialLevel());
        }

        // Validate skill level is within reasonable bounds
        if (outcome.skillInitialLevel() < 0 || outcome.skillInitialLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "skillInitialLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + outcome.skillInitialLevel());
        }

        if (outcome.newLevel() < 0 || outcome.newLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "newLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + outcome.newLevel());
        }

        // Validate damage is non-negative and reasonable
        if (outcome.damageTaken() < 0 || outcome.damageTaken() > MAX_DAMAGE) {
            throw new IllegalArgumentException(
                    "damageTaken must be between 0 and " + MAX_DAMAGE + ", got: " + outcome.damageTaken());
        }

        // Validate health boost is non-negative and reasonable
        if (outcome.healthBoost() < 0 || outcome.healthBoost() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "healthBoost must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + outcome.healthBoost());
        }

        // Validate max health increase is non-negative
        if (outcome.maxHealthIncrease() < 0 || outcome.maxHealthIncrease() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "maxHealthIncrease must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + outcome.maxHealthIncrease());
        }

        // Validate injury reduction gain is non-negative and reasonable
        if (outcome.injuryReductionGain() < 0 || outcome.injuryReductionGain() > MAX_INJURY_REDUCTION) {
            throw new IllegalArgumentException(
                    "injuryReductionGain must be between 0 and " + MAX_INJURY_REDUCTION + ", got: " + outcome.injuryReductionGain());
        }

        // Validate XP gained is non-negative
        if (outcome.xpGained() < 0) {
            throw new IllegalArgumentException("xpGained must be non-negative, got: " + outcome.xpGained());
        }

        // If canAttempt is true, these fields should be more meaningful
        if (outcome.canAttempt()) {
            if (outcome.skillInitialLevel() == 0) {
                // This is technically allowed but unusual for a successfully attempted action
                // Log a warning if needed, but don't fail validation
            }
        }
    }
}

