package org.bhp.heros_journey;

import org.springframework.stereotype.Service;

import java.util.List;

import static org.bhp.heros_journey.ExitKeyUtils.generateExitKey;

/**
 * Service for handling player movement between rooms.
 * Extracted from GameController to follow Single Responsibility Principle.
 */
@Service
public class GameMovementService {

    private final RoomGenerationService roomGenerationService;
    private final RoomRepository roomRepository;
    private final YamlLibraryService libraryService;

    public GameMovementService(RoomGenerationService roomGenerationService, RoomRepository roomRepository,
                              YamlLibraryService libraryService) {
        this.roomGenerationService = roomGenerationService;
        this.roomRepository = roomRepository;
        this.libraryService = libraryService;
    }

    /**
     * Handles player movement through an exit.
     * Manages room generation, linking, and discarding to maintain memory efficiency.
     *
     * @param exit the exit the player is moving through
     * @param state the current game state
     * @return a game response with movement result
     */
    public GameResponse handleMovement(Exit exit, GameState state) {
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
            return createResponse("Error: Could not identify the exit. Please try again.", state);
        }

        String exitKey = generateExitKey(state.getCurrentRoom().id(), exitIndex);
        String targetId = roomRepository.getLinkedRoomId(exitKey);

        // Check if the background thread has finished generating this room
        if (targetId == null || !roomRepository.exists(targetId)) {
            return createResponse("The path ahead is still forming in the mists... (Wait a moment and try again)", state);
        }

        Room nextRoom = roomRepository.getRoom(targetId);

        // TRIGGER: Start generating the next set of rooms for this new location
        // Fire-and-forget: don't block the HTTP thread waiting for room generation
        roomGenerationService.prepareAdjacentRooms(nextRoom, state.getPlayer(), roomRepository);

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

        return createResponse("You head toward " + exit.direction() + ". " + nextRoom.description(), state);
    }

    private GameResponse createResponse(String description, GameState state) {
        boolean isDead = state.isGameOver();
        RoomView view = RoomViewMapper.toRoomView(state.getCurrentRoom(), libraryService);
        return new GameResponse(description, state.getPlayer(), view, isDead);
    }
}

