package org.bhp.heros_journey;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final WorldEngine worldEngine;
    private final GameState state; // Injected session-scoped bean

    public GameController(GameService gameService, WorldEngine worldEngine, GameState state) {
        this.gameService = gameService;
        this.worldEngine = worldEngine;
        this.state = state;
    }

    @PostMapping("/action")
    public GameResponse handleAction(@RequestBody CommandRequest req) {
        // 1. Initial Start (If the game just opened)
        if (!state.isInitialized()) {
            state.setCurrentRoom(worldEngine.generateRoom("The Void"));
            worldEngine.hydrateExits(state.getCurrentRoom());
            state.setInitialized(true);
        }

        // 2. Prevent actions if the player is already dead
        if (state.isGameOver()) {
            return createResponse("You are dead. Your journey has ended.");
        }

        String cmd = req.command().trim();
        String description;

        // 3. Check for Movement (matches exit names)
        Optional<Exit> exit = findExit(cmd);
        if (exit.isPresent()) {
            description = movePlayer(exit.get());
        } else {
            // 4. Check for Interaction (Gemini Logic)
            description = gameService.processAction(cmd, state.getPlayer(), state.getCurrentRoom());
        }

        return createResponse(description);
    }

    private String movePlayer(Exit exit) {
        try {
            // Get pre-generated room from the Future
            Room nextRoom = exit.getLeadingTo().get(5, TimeUnit.SECONDS);
            state.updateRoom(nextRoom);

            // Start pre-generating the NEXT paths immediately
            worldEngine.hydrateExits(state.getCurrentRoom());

            return "You enter " + exit.getDirection() + ". " + nextRoom.description();
        } catch (Exception e) {
            return "The path is still manifesting... try again in a moment.";
        }
    }

    private GameResponse createResponse(String desc) {
        return new GameResponse(desc, state.getPlayer(), state.getCurrentRoom(), state.isGameOver());
    }
}