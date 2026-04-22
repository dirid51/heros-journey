package org.bhp.heros_journey;

import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.bhp.heros_journey.ExitKeyUtils.getRoomIdFromKey;

@Repository
@SessionScope
public class RoomRepository implements Serializable {
    private final Map<String, Room> worldMap = new ConcurrentHashMap<>();

    /**
     * Maps exit keys (roomId + exitIndex) to target room IDs.
     * This avoids mutating Exit objects and prevents race conditions.
     * Key format: "roomId:exitIndex" (e.g., "room-123:0")
     */
    private final Map<String, String> exitLinkMap = new ConcurrentHashMap<>();

    public void saveGeneratedRoom(Room room) {
        worldMap.put(room.id(), room);
    }

    public Room getRoom(String id) {
        return worldMap.get(id);
    }

    /**
     * Links an exit to a target room.
     * Thread-safe mapping that avoids mutating Exit objects.
     * @param exitKey Format: "roomId:exitIndex"
     * @param targetRoomId The ID of the target room
     */
    public void linkExit(String exitKey, String targetRoomId) {
        exitLinkMap.put(exitKey, targetRoomId);
    }

    /**
     * Retrieves the linked target room ID for an exit.
     * @param exitKey Format: "roomId:exitIndex"
     * @return The target room ID, or null if not yet linked
     */
    public String getLinkedRoomId(String exitKey) {
        return exitLinkMap.get(exitKey);
    }

    /**
     * Follows the rule: "all other generated rooms plus the previous room are discarded."
     * Keeps only the new current room and its (soon to be generated) adjacent rooms.
     */
    public void discardUnusedRooms(String currentRoomId, Set<String> adjacentRoomIds) {
        Set<String> keysToKeep = new HashSet<>(adjacentRoomIds);
        keysToKeep.add(currentRoomId);

        // Remove everything that isn't the current room or its immediate exits
        worldMap.keySet().retainAll(keysToKeep);

        // Also clean up exit links for removed rooms using the utility method
        exitLinkMap.keySet().removeIf(key -> {
            String roomId = getRoomIdFromKey(key);
            return !keysToKeep.contains(roomId);
        });
    }

    public boolean exists(String id) {
        return worldMap.containsKey(id);
    }

    // For the Save/Load requirement
    public Map<String, Room> getAllActiveRooms() {
        return worldMap;
    }
}