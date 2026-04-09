package org.bhp.heros_journey;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@SessionScope // This is the "magic" that makes it one player per browser session
@Data
public class GameState implements Serializable {
    private Player player;
    // NOSONAR: S1168 - currentRoom must be a field (not local variable) because this class is @SessionScope
    // and needs to maintain room state across multiple HTTP requests within the same player session
    private Room currentRoom;
    private boolean initialized = false;
    // In GameState:
    private final Map<String, Room> roomCache = new ConcurrentHashMap<>();

    public GameState() {
        this.player = new Player();
    }

    public void updateRoom(Room newRoom) {
        // Enforce the "One-Way" rule:
        // By overwriting the reference, the old room is gone forever.
        this.currentRoom = newRoom;
    }

    public boolean isGameOver() {
        return player.isDead();
    }
}