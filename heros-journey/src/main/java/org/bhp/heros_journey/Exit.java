package org.bhp.heros_journey;

import lombok.Data;

import java.util.concurrent.CompletableFuture;

@Data
public class Exit {
    private final String direction;
    private final String description;
    private CompletableFuture<Room> leadingTo; // The "Future" room

    public Exit(String direction, String description) {
        this.direction = direction;
        this.description = description;
    }
}