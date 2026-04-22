package org.bhp.heros_journey;

import java.util.List;

/**
 * Maps internal Room entities to UI-friendly RoomView records.
 * Handles lookup of item and NPC names from the library service.
 * Extracted from GameController to follow Single Responsibility Principle.
 */
public class RoomViewMapper {

    private RoomViewMapper() {
        // Utility class
    }

    /**
     * Converts an internal Room record into a UI-friendly RoomView.
     *
     * @param room the internal room entity
     * @param libraryService the service for looking up item and NPC details
     * @return a room view suitable for sending to the client
     */
    public static RoomView toRoomView(Room room, YamlLibraryService libraryService) {
        // Convert the list of Exit objects into just their 'direction' strings for the UI
        List<String> exitNames = room.exits().stream()
                .map(Exit::direction)
                .toList();

        // Look up the names/descriptions of items from the YAML library
        List<String> itemDescriptions = room.itemIds().stream()
                .map(id -> libraryService.getItemById(id).getName())
                .toList();

        // Look up the names of NPCs from the YAML library
        List<String> npcNames = room.npcIds().stream()
                .map(id -> libraryService.getNpcById(id).getName())
                .toList();

        return new RoomView(
                room.title(),
                room.description(),
                exitNames,
                itemDescriptions,
                npcNames
        );
    }
}
