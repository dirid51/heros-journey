package org.bhp.heros_journey;

import lombok.Getter;

/**
 * Thrown when an LLM (Large Language Model) call fails after exhausting all retry attempts.
 * This is a custom exception for domain-specific error handling in the game service.
 */
@Getter
public class LlmCallFailedException extends RuntimeException {
    private final int retryAttempts;

    public LlmCallFailedException(String message, int retryAttempts, Throwable cause) {
        super(message, cause);
        this.retryAttempts = retryAttempts;
    }

}

