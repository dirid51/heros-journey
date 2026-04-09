package org.bhp.heros_journey;

/**
 * Utility for protecting against prompt injection attacks.
 * Wraps user input in explicit delimiters to prevent injected instructions
 * from being interpreted as system instructions.
 */
public class PromptInjectionProtection {
    private static final String TRIPLE_QUOTES = "\"\"\"";

    private PromptInjectionProtection() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sanitizes user input for safe inclusion in AI prompts.
     * Wraps the input in explicit delimiters to prevent prompt injection.
     *
     * @param userInput The raw user input
     * @return Sanitized input wrapped in delimiters
     */
    public static String sanitizeForPrompt(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return TRIPLE_QUOTES + TRIPLE_QUOTES;
        }

        // Wrap in triple quotes with explicit markers
        // This makes it clear to the AI that everything between these markers
        // is literal user input, not instructions
        return TRIPLE_QUOTES + userInput + TRIPLE_QUOTES;
    }

    /**
     * Alternative method that includes a label for additional clarity
     *
     * @param userInput The raw user input
     * @return Sanitized input with label and delimiters
     */
    public static String sanitizeWithLabel(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "User typed: " + TRIPLE_QUOTES + TRIPLE_QUOTES;
        }

        return "User typed: " + TRIPLE_QUOTES + userInput + TRIPLE_QUOTES;
    }
}

