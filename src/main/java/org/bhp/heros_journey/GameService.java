package org.bhp.heros_journey;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

@Service
public class GameService {
    private final ChatClient chatClient;
    private final PromptLoader promptLoader;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500L;

    /**
     * Prompt template for action resolution.
     * Contains per-request context: the current game state and the player's action.
     * Invariant rules are in the system prompt.
     */
    private static final String ACTION_RESOLUTION_PROMPT = """
            Room: {roomDesc}
            Entities present: {entities}
            Player stats: {stats}
            Player's known skills: {skills}
            Player action: "{action}"
            
            First, check if the player's known skills list contains a skill
            applicable to this action (be reasonable with synonyms, e.g.
            'punching' matches 'Unarmed Combat'). Then resolve the action.
            
            Return JSON with all fields.
            """;

    public GameService(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.promptLoader = promptLoader;
        this.chatClient = builder
                .defaultSystem(promptLoader.getActionResolutionSystemPrompt())
                .build();
    }


    private <T> T withRetries(Callable<T> action) {
        Exception last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                if (i > 0) Thread.sleep((long) (BASE_DELAY_MS * Math.pow(2, i - 1.0)));
                return action.call();
            } catch (InterruptedException e) {
                // Re-interrupt the thread to preserve the interrupted status
                Thread.currentThread().interrupt();
                // Break out of retry loop once interrupted
                throw new LlmCallFailedException("LLM call interrupted", MAX_RETRIES, e);
            } catch (Exception e) {
                last = e;
            }
        }
        throw new LlmCallFailedException("LLM call failed after " + MAX_RETRIES + " attempts", MAX_RETRIES, last);
    }

    public String processAction(String userAction, Player player, Room currentRoom) {
        ActionOutcome outcome = withRetries(() -> chatClient.prompt()
                .user(u -> u.text(ACTION_RESOLUTION_PROMPT)
                        .param("roomDesc", currentRoom.description())
                        .param("entities", getEntityDetails(currentRoom))
                        .param("stats", player.toString())
                        .param("skills", player.getSkills().isEmpty()
                                ? "none" : player.getSkills().toString())
                        .param("action", PromptInjectionProtection.sanitizeWithLabel(userAction)))
                .call()
                .entity(ActionOutcome.class));

        // Validate outcome before applying state changes
        ActionOutcomeValidator.validate(outcome);

        if (outcome.canAttempt()) {
            applyStateChanges(player, outcome);
        }

        return outcome.description();
    }

    private void applyStateChanges(Player player, ActionOutcome outcome) {
        // 1. Update Health and Armor
        player.increaseMaxHealth(outcome.maxHealthIncrease());
        player.addCurrentHealthBoost(outcome.healthBoost());
        player.improveInjuryReduction(outcome.injuryReductionGain());
        player.receiveDamage(outcome.damageTaken());

        // 2. Single Source of Truth: Skill XP determines skill level
        // Using formula: skill_level = floor(sqrt(XP / 10))
        String skill = outcome.skillName();
        int currentXP = player.getSkillXp().getOrDefault(skill, 0);
        int newXP = currentXP + outcome.xpGained();

        player.updateSkillXp(skill, newXP);

        // Calculate new level - single source of truth
        int newLevel = (int) Math.floor(Math.sqrt(newXP / 10.0));
        player.getSkills().put(skill, newLevel);
    }

    private String getEntityDetails(Room room) {
        // Combine NPC and item IDs into a readable string for the prompt
        return "NPCs: " + room.npcIds() + ", Items: " + room.itemIds();
    }
}