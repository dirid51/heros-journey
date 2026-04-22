package org.bhp.heros_journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromptLoader Tests")
class PromptLoaderTests {

    private PromptLoader promptLoader;

    @BeforeEach
    void setUp() {
        promptLoader = new PromptLoader();
    }

    @Test
    @DisplayName("Should load action resolution system prompt")
    void testLoadPrompt() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isBlank());
    }

    @Test
    @DisplayName("Should contain core mechanics section")
    void testPromptContainsMechanics() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("CORE MECHANICS"));
    }

    @Test
    @DisplayName("Should contain skill XP rules")
    void testPromptContainsSkillXpRules() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("SKILL XP RULES"));
    }

    @Test
    @DisplayName("Should contain interaction rules")
    void testPromptContainsInteractionRules() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("INTERACTION RULES"));
    }

    @Test
    @DisplayName("Should contain narrative style guidelines")
    void testPromptContainsNarrativeStyle() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("NARRATIVE STYLE"));
    }

    @Test
    @DisplayName("Should cache prompt after first load")
    void testPromptCaching() {
        String prompt1 = promptLoader.getActionResolutionSystemPrompt();
        String prompt2 = promptLoader.getActionResolutionSystemPrompt();

        assertSame(prompt1, prompt2);
    }

    @Test
    @DisplayName("Should contain death mechanics")
    void testDeathMechanics() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("DEATH"));
    }

    @Test
    @DisplayName("Should reference skill level formula")
    void testSkillFormula() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.contains("floor(sqrt(xp / 10))"));
    }

    @Test
    @DisplayName("Should be reasonably long")
    void testPromptLength() {
        String prompt = promptLoader.getActionResolutionSystemPrompt();
        assertTrue(prompt.length() > 1000, "Prompt should be substantial");
    }

    @Test
    @DisplayName("Should handle multiple calls safely")
    void testMultipleCallsSafe() {
        for (int i = 0; i < 10; i++) {
            String prompt = promptLoader.getActionResolutionSystemPrompt();
            assertNotNull(prompt);
            assertFalse(prompt.isBlank());
        }
    }
}

