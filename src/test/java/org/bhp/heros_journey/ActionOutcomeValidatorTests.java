package org.bhp.heros_journey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ActionOutcomeValidator Tests")
class ActionOutcomeValidatorTests {

    private static final ActionOutcome VALID_OUTCOME = new ActionOutcome(
            true, "Swordsmanship", 10, false,
            50, true, 0, 0, 0, 0.0,
            null, null, null,
            "You swing your blade with increasing confidence."
    );

    @Test
    @DisplayName("Should accept valid ActionOutcome")
    void testValidOutcome() {
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(VALID_OUTCOME));
    }

    @Test
    @DisplayName("Should reject null ActionOutcome")
    void testNullOutcome() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(null)
        );
        assertEquals("ActionOutcome cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject null skillName")
    void testNullSkillName() {
        ActionOutcome outcome = new ActionOutcome(
                true, null, 10, false,
                50, true, 0, 0, 0, 0.0,
                null, null, null,
                "You swing your blade."
        );
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome)
        );
        assertTrue(ex.getMessage().contains("skillName"));
    }

    @Test
    @DisplayName("Should reject blank skillName")
    void testBlankSkillName() {
        ActionOutcome outcome = new ActionOutcome(
                true, "  ", 10, false,
                50, true, 0, 0, 0, 0.0,
                null, null, null,
                "You swing your blade."
        );
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome)
        );
        assertTrue(ex.getMessage().contains("skillName"));
    }

    @Test
    @DisplayName("Should reject null description")
    void testNullDescription() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Swordsmanship", 10, false,
                50, true, 0, 0, 0, 0.0,
                null, null, null,
                null
        );
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome)
        );
        assertTrue(ex.getMessage().contains("description"));
    }

    @Test
    @DisplayName("Should reject canAttempt=false with skillInitialLevel > 0")
    void testInconsistentAttemptFlag() {
        ActionOutcome outcome = new ActionOutcome(
                false, "Swordsmanship", 10, false,
                50, true, 0, 0, 0, 0.0,
                null, null, null,
                "You cannot do this."
        );
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome)
        );
        assertTrue(ex.getMessage().contains("canAttempt"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    @DisplayName("Should reject invalid skillInitialLevel")
    void testInvalidSkillInitialLevel(int level) {
        ActionOutcome outcome = new ActionOutcome(
                true, "Swordsmanship", level, false,
                50, true, 0, 0, 0, 0.0,
                null, null, null,
                "You swing your blade."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1001})
    @DisplayName("Should reject invalid damageTaken")
    void testInvalidDamageTaken(int damage) {
        ActionOutcome outcome = new ActionOutcome(
                true, "Dodge", 10, false,
                0, false, damage, 0, 0, 0.0,
                null, null, null,
                "You tried to dodge."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 501})
    @DisplayName("Should reject invalid healthBoost")
    void testInvalidHealthBoost(int boost) {
        ActionOutcome outcome = new ActionOutcome(
                true, "Healing", 10, false,
                0, true, 0, boost, 0, 0.0,
                null, null, null,
                "You healed."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 501})
    @DisplayName("Should reject invalid maxHealthIncrease")
    void testInvalidMaxHealthIncrease(int increase) {
        ActionOutcome outcome = new ActionOutcome(
                true, "Vitality", 10, false,
                0, true, 0, 0, increase, 0.0,
                null, null, null,
                "You gained vitality."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject negative injuryReductionGain")
    void testNegativeInjuryReductionGain() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Armor", 10, false,
                0, true, 0, 0, 0, -0.1,
                null, null, null,
                "You equipped armor."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject injuryReductionGain > 0.9")
    void testExcessiveInjuryReductionGain() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Armor", 10, false,
                0, true, 0, 0, 0, 1.0,
                null, null, null,
                "You became godlike."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject negative xpGained")
    void testNegativeXpGained() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Swordsmanship", 10, false,
                -50, true, 0, 0, 0, 0.0,
                null, null, null,
                "You swung your blade."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should accept zero values for non-applicable fields")
    void testZeroValuesAccepted() {
        ActionOutcome outcome = new ActionOutcome(
                false, "ImpossibleAction", 0, false,
                0, false, 0, 0, 0, 0.0,
                null, null, null,
                "This action is impossible."
        );
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should accept maximum valid values")
    void testMaximumValidValues() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Legendary", 100, false,
                500, true, 1000, 500, 500, 0.9,
                null, null, null,
                "You achieved legendary status."
        );
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(outcome));
    }

    // --- Inventory field tests ---

    @Test
    @DisplayName("Should accept valid itemPickedUp")
    void testValidItemPickedUp() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Scavenging", 5, false,
                20, true, 0, 0, 0, 0.0,
                "rusty_key_01", null, null,
                "You pocket the key."
        );
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should accept valid itemUsed")
    void testValidItemUsed() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Herbalism", 5, false,
                10, true, 0, 20, 0, 0.0,
                null, null, "wound_salve_01",
                "You apply the salve to your wounds."
        );
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should accept valid itemDropped")
    void testValidItemDropped() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Decision Making", 1, false,
                5, true, 0, 0, 0, 0.0,
                null, "locked_chest_01", null,
                "You leave the chest behind."
        );
        assertDoesNotThrow(() -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject blank itemPickedUp")
    void testBlankItemPickedUp() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Scavenging", 5, false,
                10, true, 0, 0, 0, 0.0,
                "   ", null, null,
                "You grab something."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject blank itemUsed")
    void testBlankItemUsed() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Herbalism", 5, false,
                10, true, 0, 20, 0, 0.0,
                null, null, "  ",
                "You use something."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }

    @Test
    @DisplayName("Should reject blank itemDropped")
    void testBlankItemDropped() {
        ActionOutcome outcome = new ActionOutcome(
                true, "Decision Making", 1, false,
                5, true, 0, 0, 0, 0.0,
                null, " ", null,
                "You drop something."
        );
        assertThrows(IllegalArgumentException.class,
                () -> ActionOutcomeValidator.validate(outcome));
    }
}