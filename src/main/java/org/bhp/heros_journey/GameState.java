package org.bhp.heros_journey;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope // This is the "magic" that makes it one player per browser session
@Data
public class GameState {
    private Player player;
    private Room currentRoom;
    private boolean initialized = false;

    public GameState() {
        this.player = new Player();
    }

    public void updateRoom(Room newRoom) {
        // Enforce the "One-Way" rule:
        // By overwriting the reference, the old room is gone forever.
        this.currentRoom = newRoom;
    }

    public boolean isGameOver() {
        return player.getCurrentHealth() < 0;
    }
}