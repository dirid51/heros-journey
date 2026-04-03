package org.bhp.heros_journey;

import java.util.List;

public record RoomView(
        String title,
        String description,
        List<String> exitNames,
        List<String> itemDescriptions,
        List<String> npcNames
) {
}