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

    public GameController(GameService gameService,
                          RoomGenerationService roomGenerationService,
                          RoomRepository roomRepository,
                          GameState state, YamlLibraryService libraryService, HttpServletRequest request) {
        this.gameService = gameService;
        this.roomGenerationService = roomGenerationService;
        this.roomRepository = roomRepository;
        this.state = state;
        this.libraryService = libraryService;
        this.request = request;
    }

    @PostMapping("/action")
    public GameResponse handleAction(@RequestBody CommandRequest req) {
        // 1. Initial Start Logic
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
                .filter(e -> cmd.contains(e.getDirection().toLowerCase()) ||
                        cmd.contains("go " + e.getDirection().toLowerCase()))
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
            roomGenerationService.prepareAdjacentRooms(startingRoom, state.getPlayer());

            return createResponse("The mists clear... " + startingRoom.description());
        } catch (Exception e) {
            log.error("Failed to start game", e);
            return createResponse("The void refuses to yield. (Error generating start room)");
        }
    }

    private GameResponse handleMovement(Exit exit) {
        String targetId = exit.getTargetRoomId();

        // Check if the background thread has finished generating this room
        if (targetId == null || !roomRepository.exists(targetId)) {
            return createResponse("The path ahead is still forming in the mists... (Wait a moment and try again)");
        }

        Room nextRoom = roomRepository.getRoom(targetId);

        // RULE: Discard old rooms to save memory and keep the journey 'one-way'
        roomRepository.discardUnusedRooms(nextRoom.id(),
                nextRoom.exits().stream().map(Exit::getTargetRoomId).collect(java.util.stream.Collectors.toSet()));

        state.updateRoom(nextRoom);

        // TRIGGER: Start generating the next set of rooms for this new location
        roomGenerationService.prepareAdjacentRooms(nextRoom, state.getPlayer());

        return createResponse("You head toward " + exit.getDirection() + ". " + nextRoom.description());
    }

    private GameResponse createResponse(String desc) {
        boolean isDead = state.getPlayer().getCurrentHealth() <= 0;

        // Map the internal Room record to the UI-friendly RoomView record
        RoomView view = createRoomView(state.getCurrentRoom());

        return new GameResponse(desc, state.getPlayer(), view, isDead);
    }

    private RoomView createRoomView(Room room) {
        // Convert the list of Exit objects into just their 'direction' strings for the UI
        List<String> exitNames = room.exits().stream()
                .map(Exit::getDirection)
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
}