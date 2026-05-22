package org.bhp.heros_journey;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

@Component
@SessionScope // This is the "magic" that makes it one player per browser session
@Getter
public class GameState implements Serializable {
    private final Player player;
    // NOSONAR: S1168 - currentRoom must be a field (not local variable) because this class is @SessionScope
    // and needs to maintain room state across multiple HTTP requests within the same player session
    private Room currentRoom;
    private boolean initialized = false;
    // TODO: NAMED NPCS - Add Map<String, String> npcHistory keyed by NPC ID to track relationship/interaction history per NPC
// TODO: COMBAT - Add CombatState activeCombat field and boolean inCombat flag; route actions through CombatService when inCombat is true
// TODO: MAP - Add a graph structure (Map<String, List<String>>) tracking visited room IDs and their connections for map visualization
// TODO: POST-RUN SUMMARY - Add run statistics fields: roomsVisited, enemiesDefeated, itemsUsed, totalDamageTaken; increment throughout the run

    public GameState() {
        this.player = new Player();
    }

    public void updateRoom(Room newRoom) {
        // Enforce the "One-Way" rule:
        // By overwriting the reference, the old room is gone forever.
        this.currentRoom = newRoom;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isGameOver() {
        return player.isDead();
    }

    public void restore(GameState saved) {
        this.player.setCurrentHealth(saved.player.getCurrentHealth());
        this.player.setMaxHealth(saved.player.getMaxHealth());
        this.player.setInjuryReduction(saved.player.getInjuryReduction());
        this.player.getSkills().putAll(saved.player.getSkills());
        saved.player.getSkillXp().forEach(this.player::updateSkillXp);
        this.currentRoom = saved.currentRoom;
        this.initialized = saved.initialized;
    }
}