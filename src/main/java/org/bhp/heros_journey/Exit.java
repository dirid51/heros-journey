package org.bhp.heros_journey;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exit {
    private String direction;   // e.g., "North", "Climb the tree"
    private String description; // The AI's creative description of the exit
    private String targetRoomId; // The ID of the room being generated in the background
}