package org.bhp.heros_journey;

/**
 * Utility class for generating and parsing exit keys.
 * Ensures consistent "roomId:exitIndex" key format across the application.
 * This centralization prevents duplication and makes maintenance easier.
 */
public class ExitKeyUtils {

    private ExitKeyUtils() {
        // Utility class, cannot be instantiated
    }

    /**
     * Generates a unique key for an exit within a room.
     * Format: "roomId:exitIndex" (e.g., "room-123:0")
     *
     * @param roomId the ID of the room
     * @param exitIndex the index of the exit within the room
     * @return a unique exit key
     */
    public static String generateExitKey(String roomId, int exitIndex) {
        return roomId + ":" + exitIndex;
    }
}

