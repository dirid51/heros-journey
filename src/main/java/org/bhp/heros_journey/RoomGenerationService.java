package org.bhp.heros_journey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import static org.bhp.heros_journey.ExitKeyUtils.generateExitKey;

@Service
public class RoomGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RoomGenerationService.class);

    private final ChatClient chatClient;
    private final YamlLibraryService libraryService;

    public RoomGenerationService(ChatClient.Builder builder, YamlLibraryService libraryService) {
        // Best Practice: Define a base system prompt to ensure JSON consistency
        this.chatClient = builder
                .defaultSystem("You are a dungeon master. Always return valid JSON matching the Room schema. " +
                        "Do not include markdown formatting like ```json in the response.")
                .build();
        this.libraryService = libraryService;
    }

    /**
     * Pre-generates rooms for all exits in the current room.
     * Uses exitLinkMap to avoid mutating Exit objects, preventing race conditions.
     * Returns a CompletableFuture that completes when all exit links have been established.
     * This ensures that discardUnusedRooms can safely query the exit map without race conditions.
     *
     * @param currentRoom the current room whose exits should be pre-generated
     * @param player the player whose skills should influence room generation
     * @param roomRepository the session-scoped repository (passed as parameter, not injected)
     */
    public CompletableFuture<Void> prepareAdjacentRooms(Room currentRoom, Player player, RoomRepository roomRepository) {
        log.info("Starting background generation for {} exits.", currentRoom.exits().size());

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int exitIndex = 0; exitIndex < currentRoom.exits().size(); exitIndex++) {
            Exit exit = currentRoom.exits().get(exitIndex);
            String exitKey = generateExitKey(currentRoom.id(), exitIndex);

            // Only generate if we haven't already
            if (roomRepository.getLinkedRoomId(exitKey) == null) {
                CompletableFuture<Void> future = generateRoomAsync(exit, player)
                        .thenAccept(generatedRoom -> {
                            // Thread-safe: use repository map instead of mutating exit
                            roomRepository.linkExit(exitKey, generatedRoom.id());
                            roomRepository.saveGeneratedRoom(generatedRoom);
                            log.debug("Room generated and linked to exit: {}", exit.direction());
                        })
                        .exceptionally(ex -> {
                            log.error("Failed to generate room for exit {}: {}", exit.direction(), ex.getMessage());
                            return null;
                        });
                futures.add(future);
            }
        }

        // Return a future that completes when all room links are established
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Async
    public CompletableFuture<Room> generateRoomAsync(Exit exit, Player player) {
        // ...existing code...
        // 1. Prepare data for the prompt
        String availableItems = String.join(", ", libraryService.getAllItemIds());
        String availableNpcs = String.join(", ", libraryService.getAllNpcIds());
        String playerSkills = player.getSkills().toString();

        // 2. Build the Prompt (Refined for better AI logic)
        String userPrompt = """
                The player is transitioning via: %s.
                Context: Player skills are %s.
                
                Task: Generate a new Room.
                1. Select up to 2 items from this library: [%s].
                2. Select up to 1 NPC from this library: [%s].
                3. Create 1-3 creative exits.
                4. Logic: If the player lacks movement skills (flight/climbing), don't provide exits requiring them.
                
                Return a JSON object with 'title', 'description', 'exits', 'npcIds', 'itemIds', and 'skillOpportunities'.
                """.formatted(exit.description(), playerSkills, availableItems, availableNpcs);

        try {
            // 3. Call Gemini
            // We generate the raw data first, then "stamp" our ID on it.
            Room rawRoom = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(Room.class);

            // Check for null response from AI model
            if (rawRoom == null) {
                throw new IllegalStateException("AI model returned null Room object");
            }

            // 4. Create the final Record with a UUID (since Records are immutable)
            Room finalizedRoom = new Room(
                    UUID.randomUUID().toString(),
                    rawRoom.title(),
                    rawRoom.description(),
                    rawRoom.exits(),
                    rawRoom.npcIds(),
                    rawRoom.itemIds(),
                    rawRoom.skillOpportunities()
            );

            return CompletableFuture.completedFuture(finalizedRoom);
        } catch (Exception e) {
            log.error("AI Generation Error: ", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}