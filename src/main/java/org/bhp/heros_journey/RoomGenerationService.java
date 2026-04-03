package org.bhp.heros_journey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RoomGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RoomGenerationService.class);

    private final ChatClient chatClient;
    private final RoomRepository roomRepository;
    private final YamlLibraryService libraryService;

    public RoomGenerationService(ChatClient.Builder builder, RoomRepository roomRepository, YamlLibraryService libraryService) {
        // Best Practice: Define a base system prompt to ensure JSON consistency
        this.chatClient = builder
                .defaultSystem("You are a dungeon master. Always return valid JSON matching the Room schema. " +
                        "Do not include markdown formatting like ```json in the response.")
                .build();
        this.roomRepository = roomRepository;
        this.libraryService = libraryService;
    }

    /**
     * Pre-generates rooms for all exits in the current room.
     */
    public void prepareAdjacentRooms(Room currentRoom, Player player) {
        log.info("Starting background generation for {} exits.", currentRoom.exits().size());

        for (Exit exit : currentRoom.exits()) {
            // Only generate if we haven't already
            if (exit.getTargetRoomId() == null) {
                generateRoomAsync(exit, player)
                        .thenAccept(generatedRoom -> {
                            exit.setTargetRoomId(generatedRoom.id());
                            roomRepository.saveGeneratedRoom(generatedRoom);
                            log.debug("Room generated and linked to exit: {}", exit.getDirection());
                        })
                        .exceptionally(ex -> {
                            log.error("Failed to generate room for exit {}: {}", exit.getDirection(), ex.getMessage());
                            return null;
                        });
            }
        }
    }

    @Async
    public CompletableFuture<Room> generateRoomAsync(Exit exit, Player player) {
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
                """.formatted(exit.getDescription(), playerSkills, availableItems, availableNpcs);

        try {
            // 3. Call Gemini
            // We generate the raw data first, then "stamp" our ID on it.
            Room rawRoom = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(Room.class);

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