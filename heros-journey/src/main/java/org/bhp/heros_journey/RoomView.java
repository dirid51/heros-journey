package org.bhp.heros_journey;

import java.util.List;

public record RoomView(
        String description,
        List<String> exitNames
) {
}