package org.bhp.heros_journey;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

@Repository
public class RoomRepository {
    private final Map<String, Room> worldMap = new ConcurrentHashMap<>();

    public void saveGeneratedRoom(Room room) {
        worldMap.put(room.id(), room);
    }

    public Room getRoom(String id) {
        return worldMap.get(id);
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
    }

    public boolean exists(String id) {
        return worldMap.containsKey(id);
    }

    // For the Save/Load requirement
    public Map<String, Room> getAllActiveRooms() {
        return worldMap;
    }
}