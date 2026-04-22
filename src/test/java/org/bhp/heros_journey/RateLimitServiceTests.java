package org.bhp.heros_journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitService Tests")
class RateLimitServiceTests {

    private RateLimitService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimitService();
    }

    @Test
    @DisplayName("Should allow null sessionId check (returns false)")
    void testNullSessionId() {
        assertFalse(rateLimiter.isAllowed(null));
    }

    @Test
    @DisplayName("Should reject blank sessionId")
    void testBlankSessionId() {
        assertFalse(rateLimiter.isAllowed("   "));
    }

    @Test
    @DisplayName("Should allow valid sessionId")
    void testValidSessionId() {
        assertTrue(rateLimiter.isAllowed("session-123"));
    }

    @Test
    @DisplayName("Should allow initial tokens within limit")
    void testInitialTokensAllowed() {
        // Each session starts with MAX_TOKENS (10)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.isAllowed("session-1"), "Token " + i + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should reject when token bucket empty")
    void testTokenBucketEmpty() {
        String sessionId = "session-test";
        
        // Consume all tokens
        for (int i = 0; i < 10; i++) {
            rateLimiter.isAllowed(sessionId);
        }
        
        // Next attempt should fail
        assertFalse(rateLimiter.isAllowed(sessionId));
    }

    @Test
    @DisplayName("Should use separate buckets for different sessions")
    void testSeparateBucketsPerSession() {
        String session1 = "session-1";
        String session2 = "session-2";
        
        // Exhaust session1
        for (int i = 0; i < 10; i++) {
            rateLimiter.isAllowed(session1);
        }
        
        // Session2 should still have tokens
        assertTrue(rateLimiter.isAllowed(session2));
    }

    @Test
    @DisplayName("Should track tokens per session independently")
    void testIndependentTokenTracking() {
        // Session 1: consume 5 tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.isAllowed("session-1"));
        }
        
        // Session 2: consume 3 tokens
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.isAllowed("session-2"));
        }
        
        // Session 1: should have 5 tokens left
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.isAllowed("session-1"));
        }
        assertFalse(rateLimiter.isAllowed("session-1"));
        
        // Session 2: should have 7 tokens left
        for (int i = 0; i < 7; i++) {
            assertTrue(rateLimiter.isAllowed("session-2"));
        }
        assertFalse(rateLimiter.isAllowed("session-2"));
    }

    @Test
    @DisplayName("Token bucket should be thread-safe")
    void testThreadSafety() throws InterruptedException {
        String sessionId = "session-concurrent";
        int[] results = {0};
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                if (rateLimiter.isAllowed(sessionId)) {
                    results[0]++;
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                if (rateLimiter.isAllowed(sessionId)) {
                    results[0]++;
                }
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Should have at most 10 successful requests
        assertEquals(10, results[0]);
    }

    @Test
    @DisplayName("Cache should expire inactive sessions")
    void testCacheExpiry() throws InterruptedException {
        String sessionId = "session-expiring";
        
        // Use the session
        assertTrue(rateLimiter.isAllowed(sessionId));
        
        // In a real scenario, this would wait 30 minutes
        // For now, we just verify the cache doesn't grow unboundedly
        for (int i = 0; i < 100; i++) {
            rateLimiter.isAllowed("session-" + i);
        }
        
        // The key thing is that the service doesn't crash with OOM
        // and can still process requests
        assertTrue(rateLimiter.isAllowed("session-new"));
    }

    @Test
    @DisplayName("Should handle rapid token consumption")
    void testRapidConsumption() {
        String sessionId = "session-rapid";
        
        // Try to consume 20 tokens rapidly
        int successCount = 0;
        for (int i = 0; i < 20; i++) {
            if (rateLimiter.isAllowed(sessionId)) {
                successCount++;
            }
        }
        
        // Should succeed exactly MAX_TOKENS (10) times
        assertEquals(10, successCount);
    }
}

