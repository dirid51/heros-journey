package org.bhp.heros_journey;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final GameMovementService gameMovementService;

    public GameController(GameService gameService,
                          RoomGenerationService roomGenerationService,
                          RoomRepository roomRepository,
                          GameState state, YamlLibraryService libraryService, HttpServletRequest request,
                          RateLimitService rateLimitService,
                          GameMovementService gameMovementService) {
        this.gameService = gameService;
        this.roomGenerationService = roomGenerationService;
        this.roomRepository = roomRepository;
        this.state = state;
        this.libraryService = libraryService;
        this.request = request;
        this.rateLimitService = rateLimitService;
        this.gameMovementService = gameMovementService;
    }

    /**
     * GET endpoint to trigger CSRF token generation and game initialization
     */
    @GetMapping("/init")
    public String initGame(HttpServletRequest request) {
        // Access the CSRF token to force Spring Security to generate and set it in a cookie
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Token is now available in the XSRF-TOKEN cookie for the frontend to use
            csrfToken.getToken();
        }
        return "{\"status\": \"initialized\"}";
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
            return gameMovementService.handleMovement(matchingExit.get(), state);
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
            state.updateRoom(startingRoom);
            state.setInitialized(true);

            // Immediately start background generation for the exits in the first room
            // Fire-and-forget: we don't need to wait for these to complete
            // The "path still forming in the mists" message handles the case where rooms aren't ready yet
            roomGenerationService.prepareAdjacentRooms(startingRoom, state.getPlayer(), roomRepository);

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

    private GameResponse createResponse(String desc) {
        boolean isDead = state.isGameOver();

        // Map the internal Room record to the UI-friendly RoomView record
        RoomView view = RoomViewMapper.toRoomView(state.getCurrentRoom(), libraryService);

        return new GameResponse(desc, state.getPlayer(), view, isDead);
    }
}