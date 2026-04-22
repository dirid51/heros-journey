package org.bhp.heros_journey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExitKeyUtils Tests")
class ExitKeyUtilsTests {

    @Test
    @DisplayName("Should generate exit key in correct format")
    void testGenerateExitKey() {
        String key = ExitKeyUtils.generateExitKey("room-123", 0);
        assertEquals("room-123:0", key);
    }

    @ParameterizedTest
    @CsvSource({
            "room-123, 0, room-123:0",
            "room-456, 1, room-456:1",
            "uuid-abcdef, 5, uuid-abcdef:5",
            "starting-room, 2, starting-room:2"
    })
    @DisplayName("Should generate correct exit keys")
    void testGenerateExitKeyVariations(String roomId, int exitIndex, String expected) {
        String result = ExitKeyUtils.generateExitKey(roomId, exitIndex);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should extract room ID from exit key")
    void testGetRoomIdFromKey() {
        String roomId = ExitKeyUtils.getRoomIdFromKey("room-123:0");
        assertEquals("room-123", roomId);
    }

    @ParameterizedTest
    @CsvSource({
            "room-123:0, room-123",
            "room-456:1, room-456",
            "uuid-abcdef:5, uuid-abcdef",
            "starting-room:2, starting-room"
    })
    @DisplayName("Should extract room IDs correctly")
    void testGetRoomIdFromKeyVariations(String exitKey, String expectedRoomId) {
        String result = ExitKeyUtils.getRoomIdFromKey(exitKey);
        assertEquals(expectedRoomId, result);
    }

    @Test
    @DisplayName("Should handle null exit key")
    void testGetRoomIdFromKeyNull() {
        String result = ExitKeyUtils.getRoomIdFromKey(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle exit key without separator")
    void testGetRoomIdFromKeyNoSeparator() {
        String result = ExitKeyUtils.getRoomIdFromKey("room-123");
        assertEquals("room-123", result);
    }

    @Test
    @DisplayName("Should use last occurrence of separator")
    void testGetRoomIdFromKeyMultipleSeparators() {
        // If a room ID happened to contain colons (unlikely but defensive)
        String result = ExitKeyUtils.getRoomIdFromKey("room:with:colons:5");
        assertEquals("room:with:colons", result);
    }

    @Test
    @DisplayName("Should round-trip: generate and extract room ID")
    void testRoundTrip() {
        String originalRoomId = "room-xyz";
        int exitIndex = 3;

        // Generate key
        String key = ExitKeyUtils.generateExitKey(originalRoomId, exitIndex);

        // Extract room ID
        String extractedRoomId = ExitKeyUtils.getRoomIdFromKey(key);

        assertEquals(originalRoomId, extractedRoomId);
    }

    @Test
    @DisplayName("Should handle UUIDs as room IDs")
    void testUUIDRoomIds() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String key = ExitKeyUtils.generateExitKey(uuid, 0);

        String extracted = ExitKeyUtils.getRoomIdFromKey(key);
        assertEquals(uuid, extracted);
    }

    @Test
    @DisplayName("Should handle large exit indices")
    void testLargeExitIndices() {
        String key = ExitKeyUtils.generateExitKey("room-123", 999);
        assertEquals("room-123:999", key);

        String extracted = ExitKeyUtils.getRoomIdFromKey(key);
        assertEquals("room-123", extracted);
    }

    @Test
    @DisplayName("Should generate consistent keys")
    void testKeyConsistency() {
        String key1 = ExitKeyUtils.generateExitKey("room-1", 0);
        String key2 = ExitKeyUtils.generateExitKey("room-1", 0);

        assertEquals(key1, key2);
    }
}

