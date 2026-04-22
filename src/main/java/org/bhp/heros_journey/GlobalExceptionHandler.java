package org.bhp.heros_journey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global exception handler for the game controller.
 * Converts application-level exceptions into graceful game messages.
 */
@ControllerAdvice
@RestController
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles InvalidActionOutcomeException by converting it to a graceful game message.
     * This prevents AI model errors from appearing as HTTP 500 errors to the player.
     *
     * @param ex the exception thrown
     * @return a game response with a narrative explanation
     */
    @ExceptionHandler(InvalidActionOutcomeException.class)
    public GameResponse handleInvalidActionOutcome(InvalidActionOutcomeException ex) {
        log.warn("Invalid action outcome from AI model: {}", ex.getMessage());

        // Return a narrative reason rather than exposing the technical error
        GameResponse errorResponse = new GameResponse(
                "Reality warps strangely around you. The moment passes, leaving you unharmed.",
                null,
                null,
                false
        );

        return errorResponse;
    }

    /**
     * Fallback handler for unexpected exceptions to prevent 500 errors from leaking details.
     *
     * @param ex the exception thrown
     * @return a game response with a generic narrative message
     */
    @ExceptionHandler(Exception.class)
    public GameResponse handleGenericException(Exception ex) {
        log.error("Unexpected error during action processing", ex);

        return new GameResponse(
                "The world seems to pause for a moment. Reality reasserts itself.",
                null,
                null,
                false
        );
    }
}
