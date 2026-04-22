package org.bhp.heros_journey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket rate limiter for per-session API rate limiting.
 * Prevents API quota exhaustion from users spamming commands.
 */
@Service
public class RateLimitService {

    // Token bucket configuration
    private static final long TOKENS_PER_SECOND = 2; // 2 requests per second per session
    private static final long MAX_TOKENS = 10; // Max tokens to accumulate
    private static final long REFILL_INTERVAL_MS = 1000 / TOKENS_PER_SECOND; // Refill every 500ms

    private static class TokenBucket {
        private final AtomicLong lastRefillTime;
        private final AtomicLong tokens;

        TokenBucket() {
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.tokens = new AtomicLong(MAX_TOKENS);
        }

        boolean tryConsume() {
            refillTokens();
            return tokens.decrementAndGet() >= 0;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long previousRefillTime = lastRefillTime.get();
            long timePassed = now - previousRefillTime;

            if (timePassed >= REFILL_INTERVAL_MS && lastRefillTime.compareAndSet(previousRefillTime, now)) {
                long tokensToAdd = (timePassed / REFILL_INTERVAL_MS) * TOKENS_PER_SECOND;
                tokens.updateAndGet(current -> Math.min(current + tokensToAdd, MAX_TOKENS));
            }
        }
    }

    /**
     * Session buckets cache with 30-minute expiry after last access.
     * Prevents unbounded growth of inactive session buckets.
     */
    private final Cache<String, TokenBucket> sessionBuckets = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    /**
     * Check if a session is allowed to make a request.
     *
     * @param sessionId The session identifier
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        TokenBucket bucket = sessionBuckets.get(sessionId, _ -> new TokenBucket());
        return bucket.tryConsume();
    }
}

