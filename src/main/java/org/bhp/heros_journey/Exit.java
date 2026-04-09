package org.bhp.heros_journey;

import java.io.Serializable;

/**
 * Immutable record representing an exit from a room.
 * Exit links to target rooms are managed separately in RoomRepository.exitLinkMap
 * to avoid race conditions between background generation and concurrent movement.
 */
public record Exit(
        String direction,   // e.g., "North", "Climb the tree"
        String description  // The AI's creative description of the exit
) implements Serializable {
}