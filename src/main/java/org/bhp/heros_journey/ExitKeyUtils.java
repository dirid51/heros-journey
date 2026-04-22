package org.bhp.heros_journey;

/**
 * Utility class for generating and parsing exit keys.
 * Ensures consistent "roomId:exitIndex" key format across the application.
 * This centralization prevents duplication and makes maintenance easier.
 */
public class ExitKeyUtils {

    private static final String SEPARATOR = ":";

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
        return roomId + SEPARATOR + exitIndex;
    }

    /**
     * Extracts the room ID from an exit key.
     * Format: "roomId:exitIndex" -> returns "roomId"
     *
     * @param exitKey the exit key in format "roomId:exitIndex"
     * @return the room ID, or the original key if parsing fails
     */
    public static String getRoomIdFromKey(String exitKey) {
        if (exitKey == null || !exitKey.contains(SEPARATOR)) {
            return exitKey;
        }
        int separatorIndex = exitKey.lastIndexOf(SEPARATOR);
        return exitKey.substring(0, separatorIndex);
    }
}

