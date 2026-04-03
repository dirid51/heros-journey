package org.bhp.heros_journey;

import java.util.List;

public record Room(
        String id, // Add this to track the room in your cache/repository
        String title,
        String description,
        List<Exit> exits,
        List<String> npcIds,  // IDs from your NPCs YAML
        List<String> itemIds, // IDs from your Items YAML
        List<String> skillOpportunities
) {
}