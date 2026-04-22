package org.bhp.heros_journey;

/**
 * Validator for ActionEligibility and ActionResult records to prevent AI model from silently corrupting game state.
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
     * Validates an ActionEligibility record for consistency and reasonable values.
     *
     * @param eligibility the ActionEligibility to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateEligibility(ActionEligibility eligibility) {
        if (eligibility == null) {
            throw new IllegalArgumentException("ActionEligibility cannot be null");
        }

        // Required fields that must always be present
        if (eligibility.skillName() == null || eligibility.skillName().isBlank()) {
            throw new IllegalArgumentException("ActionEligibility.skillName must not be empty");
        }

        if (eligibility.description() == null || eligibility.description().isBlank()) {
            throw new IllegalArgumentException("ActionEligibility.description must not be empty");
        }

        // Validate canAttempt consistency
        if (!eligibility.canAttempt() && eligibility.skillInitialLevel() > 0) {
            throw new IllegalArgumentException(
                    "If canAttempt is false, skillInitialLevel must be 0, got: " + eligibility.skillInitialLevel());
        }

        // Validate skill level is within reasonable bounds
        if (eligibility.skillInitialLevel() < 0 || eligibility.skillInitialLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "skillInitialLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + eligibility.skillInitialLevel());
        }
    }

    /**
     * Validates an ActionResult record for consistency and reasonable values.
     * Should only be called if the action was successfully attempted (canAttempt=true).
     *
     * @param result the ActionResult to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateResult(ActionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ActionResult cannot be null");
        }

        if (result.description() == null || result.description().isBlank()) {
            throw new IllegalArgumentException("ActionResult.description must not be empty");
        }

        // Validate skill level is within reasonable bounds
        if (result.newLevel() < 0 || result.newLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "newLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + result.newLevel());
        }

        // Validate damage is non-negative and reasonable
        if (result.damageTaken() < 0 || result.damageTaken() > MAX_DAMAGE) {
            throw new IllegalArgumentException(
                    "damageTaken must be between 0 and " + MAX_DAMAGE + ", got: " + result.damageTaken());
        }

        // Validate health boost is non-negative and reasonable
        if (result.healthBoost() < 0 || result.healthBoost() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "healthBoost must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + result.healthBoost());
        }

        // Validate max health increase is non-negative
        if (result.maxHealthIncrease() < 0 || result.maxHealthIncrease() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "maxHealthIncrease must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + result.maxHealthIncrease());
        }

        // Validate injury reduction gain is non-negative and reasonable
        if (result.injuryReductionGain() < 0 || result.injuryReductionGain() > MAX_INJURY_REDUCTION) {
            throw new IllegalArgumentException(
                    "injuryReductionGain must be between 0 and " + MAX_INJURY_REDUCTION + ", got: " + result.injuryReductionGain());
        }

        // Validate XP gained is non-negative
        if (result.xpGained() < 0) {
            throw new IllegalArgumentException("xpGained must be non-negative, got: " + result.xpGained());
        }
    }
}
