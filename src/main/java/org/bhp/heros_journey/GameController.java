package org.bhp.heros_journey;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;
    private final RoomGenerationService roomGenerationService;
    private final RoomRepository roomRepository;
    private final GameState state;
    private final YamlLibraryService libraryService;
    private final HttpServletRequest request;
    private final RateLimitService rateLimitService;

    public GameController(GameService gameService,
                          RoomGenerationService roomGenerationService,
                          RoomRepository roomRepository,
                          GameState state, YamlLibraryService libraryService, HttpServletRequest request,
                          RateLimitService rateLimitService) {
        this.gameService = gameService;
        this.roomGenerationService = roomGenerationService;
        this.roomRepository = roomRepository;
        this.state = state;
        this.libraryService = libraryService;
        this.request = request;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/action")
    public GameResponse handleAction(@RequestBody CommandRequest req) {
        // 1. Rate Limiting Check - Prevent API quota exhaustion
        String sessionId = request.getSession().getId();
        if (!rateLimitService.isAllowed(sessionId)) {
            return createResponse("You attempt to act, but the world seems to slow around you. Try again in a moment.");
        }

        // 2. Initial Start Logic
        if (!state.isInitialized()) {
            return startNewGame();
        }

        if (state.isGameOver()) {
            return createResponse("You have fallen. Your journey ends here.");
        }

        String cmd = req.command().trim().toLowerCase();
        if (cmd.isBlank() || cmd.length() > 500) {
            return createResponse("Please enter a valid action (max 500 characters).");
        }

        // 2. Check for Movement (matches direction or exit description)
        Optional<Exit> matchingExit = state.getCurrentRoom().exits().stream()
                .filter(e -> cmd.contains(e.direction().toLowerCase()) ||
                        cmd.contains("go " + e.direction().toLowerCase()))
                .findFirst();

        if (matchingExit.isPresent()) {
            return handleMovement(matchingExit.get());
        }

        // 3. Narrative/Action Interaction (AI Logic)
        String narrative = gameService.processAction(cmd, state.getPlayer(), state.getCurrentRoom());
        return createResponse(narrative);
    }

    private GameResponse startNewGame() {
        request.changeSessionId(); // Start a new session for this player

        // Generate the very first room synchronously so the player has somewhere to stand
        // We pass a dummy exit or a 'starting' prompt
        Exit startExit = new Exit("The Beginning", "A mysterious starting point");
        try {
            Room startingRoom = roomGenerationService.generateRoomAsync(startExit, state.getPlayer()).get();
            state.setCurrentRoom(startingRoom);
            state.setInitialized(true);

            // Immediately start background generation for the exits in the first room
            // Fire-and-forget: we don't need to wait for these to complete
            roomGenerationService.prepareAdjacentRooms(startingRoom, state.getPlayer());

            return createResponse("The mists clear... " + startingRoom.description());
        } catch (InterruptedException e) {
            // Re-interrupt the thread as per best practice when InterruptedException is caught
            Thread.currentThread().interrupt();
            log.error("Game initialization interrupted", e);
            return createResponse("The void refuses to yield. (Error generating start room)");
        } catch (Exception e) {
            log.error("Failed to start game", e);
            return createResponse("The void refuses to yield. (Error generating start room)");
        }
    }

    private GameResponse handleMovement(Exit exit) {
        // Query the repository for the linked target room ID using the exit key
        // Use counter-based loop to find exit index instead of indexOf() to avoid
        // issues with duplicate Exit objects that may be equal by .equals()
        List<Exit> currentExits = state.getCurrentRoom().exits();
        int exitIndex = -1;
        for (int i = 0; i < currentExits.size(); i++) {
            if (currentExits.get(i) == exit) {  // Identity check for the exact Exit object
                exitIndex = i;
                break;
            }
        }
        if (exitIndex == -1) {
            return createResponse("Error: Could not identify the exit. Please try again.");
        }

        String exitKey = generateExitKey(state.getCurrentRoom().id(), exitIndex);
        String targetId = roomRepository.getLinkedRoomId(exitKey);

        // Check if the background thread has finished generating this room
        if (targetId == null || !roomRepository.exists(targetId)) {
            return createResponse("The path ahead is still forming in the mists... (Wait a moment and try again)");
        }

        Room nextRoom = roomRepository.getRoom(targetId);

        // TRIGGER: Start generating the next set of rooms for this new location
        // Wait for all exit links to be established before discarding rooms
        try {
            roomGenerationService.prepareAdjacentRooms(nextRoom, state.getPlayer()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Room generation interrupted", e);
        } catch (Exception e) {
            log.error("Error generating adjacent rooms", e);
            // Continue anyway - worst case, rooms won't be pre-generated
        }

        // RULE: Discard old rooms to save memory and keep the journey 'one-way'
        // Safe to query exit links now that adjacent rooms are being generated
        // Use counter-based loop instead of indexOf() to avoid matching by .equals()
        java.util.Set<String> linkedRoomIds = new java.util.HashSet<>();
        List<Exit> nextRoomExits = nextRoom.exits();
        for (int i = 0; i < nextRoomExits.size(); i++) {
            String key = generateExitKey(nextRoom.id(), i);
            String linkedId = roomRepository.getLinkedRoomId(key);
            if (linkedId != null) {
                linkedRoomIds.add(linkedId);
            }
        }
        roomRepository.discardUnusedRooms(nextRoom.id(), linkedRoomIds);

        state.updateRoom(nextRoom);


        return createResponse("You head toward " + exit.direction() + ". " + nextRoom.description());
    }

    private GameResponse createResponse(String desc) {
        boolean isDead = state.getPlayer().getCurrentHealth() <= 0;

        // Map the internal Room record to the UI-friendly RoomView record
        RoomView view = createRoomView(state.getCurrentRoom());

        return new GameResponse(desc, state.getPlayer(), view, isDead);
    }

    private RoomView createRoomView(Room room) {
        // ...existing code...
        // Convert the list of Exit objects into just their 'direction' strings for the UI
        List<String> exitNames = room.exits().stream()
                .map(Exit::direction)
                .toList();

        // Look up the names/descriptions of items from your YAML library
        List<String> itemDescriptions = room.itemIds().stream()
                .map(id -> libraryService.getItemById(id).getName())
                .toList();

        // Look up the names of NPCs from your YAML library
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

    /**
     * Generates a unique key for an exit within a room.
     * Format: "roomId:exitIndex"
     */
    private String generateExitKey(String roomId, int exitIndex) {
        return roomId + ":" + exitIndex;
    }
}