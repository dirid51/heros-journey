package org.bhp.heros_journey;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads prompt templates from external resource files.
 * This allows prompts to be maintained separately from code and versioned independently.
 */
@Component
public class PromptLoader {

    private volatile String actionResolutionSystemPrompt;

    public String getActionResolutionSystemPrompt() {
        if (actionResolutionSystemPrompt == null) {
            synchronized (this) {
                if (actionResolutionSystemPrompt == null) {
                    try {
                        ClassPathResource resource = new ClassPathResource("prompts/action-resolution-system.txt");
                        byte[] bytes = resource.getInputStream().readAllBytes();
                        actionResolutionSystemPrompt = new String(bytes, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load action resolution system prompt", e);
                    }
                }
            }
        }
        return actionResolutionSystemPrompt;
    }
}

