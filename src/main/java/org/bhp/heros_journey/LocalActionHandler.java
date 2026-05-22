package org.bhp.heros_journey;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Handles player commands that don't require LLM reasoning.
 * Intercepts simple introspective or idle commands before they reach the AI,
 * reducing latency and model load for actions with deterministic responses.
 */
@Component
public class LocalActionHandler {

    // Commands that just describe the current room state
    private static final Set<String> LOOK_COMMANDS = Set.of(
            "look", "look around", "examine room", "survey", "survey room",
            "inspect", "check surroundings", "where am i", "what's here",
            "whats here", "describe room", "observe"
    );

    // Commands that return player status
    private static final Set<String> STATUS_COMMANDS = Set.of(
            "status", "stats", "check stats", "check status", "inventory",
            "check health", "health", "my stats", "player stats", "skills",
            "check skills", "what do i have", "what do i know"
    );

    // Commands that trigger a rest/wait with deterministic outcome
    private static final Set<String> REST_COMMANDS = Set.of(
            "wait", "rest", "catch my breath", "catch breath",
            "pause", "stand still", "do nothing"
    );
    // TODO: INVENTORY - Add item use handling here for common items (e.g. healing potions, keys) before falling through to LLM

    // Tiny passive heal when resting — keeps the player engaged without LLM cost
    private static final int REST_HEAL_AMOUNT = 1;

    /**
     * Attempts to handle a command locally without calling the LLM.
     *
     * @param cmd    the player's command (already lowercased and trimmed)
     * @param player the current player state
     * @param room   the current room
     * @return a response string if handled locally, or empty to fall through to the LLM
     */
    public Optional<String> tryHandle(String cmd, Player player, Room room) {
        if (matchesAny(cmd, LOOK_COMMANDS)) {
            return Optional.of(handleLook(room));
        }
        if (matchesAny(cmd, STATUS_COMMANDS)) {
            return Optional.of(handleStatus(player));
        }
        if (matchesAny(cmd, REST_COMMANDS)) {
            return Optional.of(handleRest(player));
        }
        return Optional.empty();
    }

    private String handleLook(Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append(room.description());

        if (!room.npcIds().isEmpty()) {
            sb.append("\n\nPresent here: ").append(String.join(", ", room.npcIds())).append(".");
        }
        if (!room.itemIds().isEmpty()) {
            sb.append("\nYou notice: ").append(String.join(", ", room.itemIds())).append(".");
        }
        if (!room.exits().isEmpty()) {
            sb.append("\nPossible paths: ");
            sb.append(room.exits().stream()
                    .map(Exit::direction)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
            sb.append(".");
        }
        if (room.skillOpportunities() != null && !room.skillOpportunities().isEmpty()) {
            sb.append("\nYou sense opportunities here: ")
                    .append(String.join(", ", room.skillOpportunities())).append(".");
        }
        return sb.toString();
    }

    private String handleStatus(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Health: %d / %d", player.getCurrentHealth(), player.getMaxHealth()));
        sb.append(String.format("%nInjury Reduction: %.0f%%", player.getInjuryReduction() * 100));

        if (player.getSkills().isEmpty()) {
            sb.append("\nSkills: none yet.");
        } else {
            sb.append("\nSkills:");
            player.getSkills().forEach((skill, level) ->
                    sb.append(String.format("%n  %s: %d", skill, level)));
        }
        return sb.toString();
    }

    private String handleRest(Player player) {
        if (player.getCurrentHealth() >= player.getMaxHealth()) {
            return "You take a moment to steady yourself. You are already at full strength — " +
                    "resting further would be a luxury the world may not afford you.";
        }
        player.addCurrentHealthBoost(REST_HEAL_AMOUNT);
        return String.format(
                "You pause and catch your breath. The brief rest restores %d health. " +
                        "You feel steadier, though the path ahead remains uncertain.",
                REST_HEAL_AMOUNT);
    }

    /**
     * Checks if the command exactly matches or contains any keyword from the set.
     * The "contains" check handles natural phrasing like "I want to look around".
     */
    private boolean matchesAny(String cmd, Set<String> keywords) {
        if (keywords.contains(cmd)) {
            return true;
        }
        // Also match if the command contains a keyword as a phrase
        // e.g. "i look around the room" matches "look around"
        return keywords.stream().anyMatch(cmd::contains);
    }
}