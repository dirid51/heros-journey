package org.bhp.heros_journey;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class WorldEngine {

    private final ChatClient chatClient;

    public WorldEngine(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                                You are a world-building engine for a procedural, text-based RPG.
                                Your task is to generate immersive rooms based on brief descriptions
                                of exits leading to them. Each room should have a unique atmosphere
                                and distinct features, and should include 2-4 exits with creative
                                descriptions. Always ensure that the generated room is different
                                from any previous rooms to maintain a sense of discovery.
                        """)
                .build();
    }

    @Async
    public CompletableFuture<Room> generateRoomAsync(String exitDescription) {
        Room room = chatClient.prompt()
                .user("Generate a new room based on this entry point: " + exitDescription)
                .call()
                .entity(Room.class); // Spring AI parses the JSON

        return CompletableFuture.completedFuture(room);
    }

    public void hydrateExits(Room currentRoom) {
        for (Exit exit : currentRoom.exits()) {
            // Start generating all rooms in parallel!
            exit.setLeadingTo(generateRoomAsync(exit.getDescription()));
        }
    }

    public String movePlayer(String direction, Player player) {
        Exit chosenExit = findExit(direction);

        try {
            // .get() will wait for Gemini to finish IF it's not done yet.
            // If it's already done, this is instantaneous.
            Room nextRoom = chosenExit.getLeadingTo().get(5, TimeUnit.SECONDS);

            this.currentRoom = nextRoom;

            // Start pre-generating the NEXT set of exits immediately!
            hydrateExits(this.currentRoom);

            return "You enter " + chosenExit.getDirection() + ". " + currentRoom.getDescription();
        } catch (Exception e) {
            return "The path is still forming in the mist... (Try again in a second).";
        }
    }
}