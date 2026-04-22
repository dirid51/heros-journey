package org.bhp.heros_journey;

/**
 * Validator for ActionOutcome (and its constituent ActionEligibility / ActionResult records)
 * to prevent the AI model from silently corrupting game state.
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
     * Validates a combined ActionOutcome record for consistency and reasonable values.
     * This is the primary validation path used when the AI returns a single unified response.
     *
     * @param outcome the ActionOutcome to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validate(ActionOutcome outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("ActionOutcome cannot be null");
        }

        // --- Fields that must always be present ---
        if (outcome.skillName() == null || outcome.skillName().isBlank()) {
            throw new IllegalArgumentException("ActionOutcome.skillName must not be empty");
        }
        if (outcome.description() == null || outcome.description().isBlank()) {
            throw new IllegalArgumentException("ActionOutcome.description must not be empty");
        }

        // canAttempt=false means the action was blocked; skill level must be 0 in that case
        if (!outcome.canAttempt() && outcome.skillInitialLevel() > 0) {
            throw new IllegalArgumentException(
                    "If canAttempt is false, skillInitialLevel must be 0, got: " + outcome.skillInitialLevel());
        }

        // --- Bounds checks (apply regardless of canAttempt) ---
        if (outcome.skillInitialLevel() < 0 || outcome.skillInitialLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "skillInitialLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + outcome.skillInitialLevel());
        }

        // --- Result-phase fields (only meaningful when canAttempt=true, but still bounded) ---
        if (outcome.newLevel() < 0 || outcome.newLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "newLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + outcome.newLevel());
        }
        if (outcome.damageTaken() < 0 || outcome.damageTaken() > MAX_DAMAGE) {
            throw new IllegalArgumentException(
                    "damageTaken must be between 0 and " + MAX_DAMAGE + ", got: " + outcome.damageTaken());
        }
        if (outcome.healthBoost() < 0 || outcome.healthBoost() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "healthBoost must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + outcome.healthBoost());
        }
        if (outcome.maxHealthIncrease() < 0 || outcome.maxHealthIncrease() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "maxHealthIncrease must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + outcome.maxHealthIncrease());
        }
        if (outcome.injuryReductionGain() < 0 || outcome.injuryReductionGain() > MAX_INJURY_REDUCTION) {
            throw new IllegalArgumentException(
                    "injuryReductionGain must be between 0 and " + MAX_INJURY_REDUCTION + ", got: " + outcome.injuryReductionGain());
        }
        if (outcome.xpGained() < 0) {
            throw new IllegalArgumentException("xpGained must be non-negative, got: " + outcome.xpGained());
        }
    }

    /**
     * Validates an ActionEligibility record for consistency and reasonable values.
     * Used by GameService when the two-phase (eligibility → result) AI flow is active.
     *
     * @param eligibility the ActionEligibility to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateEligibility(ActionEligibility eligibility) {
        if (eligibility == null) {
            throw new IllegalArgumentException("ActionEligibility cannot be null");
        }

        if (eligibility.skillName() == null || eligibility.skillName().isBlank()) {
            throw new IllegalArgumentException("ActionEligibility.skillName must not be empty");
        }
        if (eligibility.description() == null || eligibility.description().isBlank()) {
            throw new IllegalArgumentException("ActionEligibility.description must not be empty");
        }
        if (!eligibility.canAttempt() && eligibility.skillInitialLevel() > 0) {
            throw new IllegalArgumentException(
                    "If canAttempt is false, skillInitialLevel must be 0, got: " + eligibility.skillInitialLevel());
        }
        if (eligibility.skillInitialLevel() < 0 || eligibility.skillInitialLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "skillInitialLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + eligibility.skillInitialLevel());
        }
    }

    /**
     * Validates an ActionResult record for consistency and reasonable values.
     * Used by GameService when the two-phase (eligibility → result) AI flow is active.
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
        if (result.newLevel() < 0 || result.newLevel() > MAX_SKILL_LEVEL) {
            throw new IllegalArgumentException(
                    "newLevel must be between 0 and " + MAX_SKILL_LEVEL + ", got: " + result.newLevel());
        }
        if (result.damageTaken() < 0 || result.damageTaken() > MAX_DAMAGE) {
            throw new IllegalArgumentException(
                    "damageTaken must be between 0 and " + MAX_DAMAGE + ", got: " + result.damageTaken());
        }
        if (result.healthBoost() < 0 || result.healthBoost() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "healthBoost must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + result.healthBoost());
        }
        if (result.maxHealthIncrease() < 0 || result.maxHealthIncrease() > MAX_HEALTH_BOOST) {
            throw new IllegalArgumentException(
                    "maxHealthIncrease must be between 0 and " + MAX_HEALTH_BOOST + ", got: " + result.maxHealthIncrease());
        }
        if (result.injuryReductionGain() < 0 || result.injuryReductionGain() > MAX_INJURY_REDUCTION) {
            throw new IllegalArgumentException(
                    "injuryReductionGain must be between 0 and " + MAX_INJURY_REDUCTION + ", got: " + result.injuryReductionGain());
        }
        if (result.xpGained() < 0) {
            throw new IllegalArgumentException("xpGained must be non-negative, got: " + result.xpGained());
        }
    }
}