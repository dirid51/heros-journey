package org.bhp.heros_journey;

public record GameResponse(
        String description,
        Player player,
        RoomView currentRoom,
        boolean isGameOver
) {
}