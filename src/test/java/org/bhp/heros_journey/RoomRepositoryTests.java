package org.bhp.heros_journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoomRepository Tests")
class RoomRepositoryTests {

    private RoomRepository repository;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        repository = new RoomRepository();
        testRoom = new Room(
                "room-1",
                "A test room",
                "This is a test room.",
                new ArrayList<>(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    @DisplayName("Should save and retrieve room")
    void testSaveAndGetRoom() {
        repository.saveGeneratedRoom(testRoom);

        Room retrieved = repository.getRoom("room-1");
        assertNotNull(retrieved);
        assertEquals("room-1", retrieved.id());
    }

    @Test
    @DisplayName("Should return null for non-existent room")
    void testGetNonExistentRoom() {
        Room retrieved = repository.getRoom("non-existent");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Should check room existence")
    void testRoomExists() {
        repository.saveGeneratedRoom(testRoom);

        assertTrue(repository.exists("room-1"));
        assertFalse(repository.exists("non-existent"));
    }

    @Test
    @DisplayName("Should link and retrieve exit")
    void testLinkAndGetExit() {
        repository.linkExit("room-1:0", "room-2");

        String linkedId = repository.getLinkedRoomId("room-1:0");
        assertEquals("room-2", linkedId);
    }

    @Test
    @DisplayName("Should return null for non-linked exit")
    void testGetNonLinkedExit() {
        String linkedId = repository.getLinkedRoomId("room-1:0");
        assertNull(linkedId);
    }

    @Test
    @DisplayName("Should discard unused rooms")
    void testDiscardUnusedRooms() {
        // Save multiple rooms
        repository.saveGeneratedRoom(testRoom);
        Room room2 = new Room("room-2", "Title", "Desc", new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(room2);
        Room room3 = new Room("room-3", "Title", "Desc", new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(room3);

        // Discard: keep only room-1 and room-2
        repository.discardUnusedRooms("room-1", Set.of("room-2"));

        assertTrue(repository.exists("room-1"));
        assertTrue(repository.exists("room-2"));
        assertFalse(repository.exists("room-3"));
    }

    @Test
    @DisplayName("Should clean up exit links for discarded rooms")
    void testDiscardExitLinks() {
        // Create rooms and exits
        repository.saveGeneratedRoom(testRoom);
        Room room2 = new Room("room-2", "Title", "Desc", new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(room2);

        // Link exits
        repository.linkExit("room-1:0", "room-2");
        repository.linkExit("room-3:0", "room-4");

        // Discard room-3 (and room-4)
        repository.discardUnusedRooms("room-1", Set.of("room-2"));

        // Exit from room-3 should be cleaned up
        assertNull(repository.getLinkedRoomId("room-3:0"));
        // But exit from room-1 should remain
        assertEquals("room-2", repository.getLinkedRoomId("room-1:0"));
    }

    @Test
    @DisplayName("Should handle multiple exits from same room")
    void testMultipleExitsSameRoom() {
        repository.linkExit("room-1:0", "room-2");
        repository.linkExit("room-1:1", "room-3");
        repository.linkExit("room-1:2", "room-4");

        assertEquals("room-2", repository.getLinkedRoomId("room-1:0"));
        assertEquals("room-3", repository.getLinkedRoomId("room-1:1"));
        assertEquals("room-4", repository.getLinkedRoomId("room-1:2"));
    }

    @Test
    @DisplayName("Should allow updating exit link")
    void testUpdateExitLink() {
        repository.linkExit("room-1:0", "room-2");
        assertEquals("room-2", repository.getLinkedRoomId("room-1:0"));

        // Update the link
        repository.linkExit("room-1:0", "room-3");
        assertEquals("room-3", repository.getLinkedRoomId("room-1:0"));
    }

    @Test
    @DisplayName("Should get all active rooms")
    void testGetAllActiveRooms() {
        repository.saveGeneratedRoom(testRoom);
        Room room2 = new Room("room-2", "Title", "Desc", new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(room2);

        Map<String, Room> allRooms = repository.getAllActiveRooms();
        assertEquals(2, allRooms.size());
        assertTrue(allRooms.containsKey("room-1"));
        assertTrue(allRooms.containsKey("room-2"));
    }

    @Test
    @DisplayName("Should handle empty repository")
    void testEmptyRepository() {
        assertNull(repository.getRoom("any-room"));
        assertFalse(repository.exists("any-room"));
        assertTrue(repository.getAllActiveRooms().isEmpty());
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void testThreadSafety() throws InterruptedException {
        // Create rooms
        for (int i = 0; i < 10; i++) {
            Room room = new Room("room-" + i, "Title", "Desc", new ArrayList<>(), List.of(), List.of(), List.of());
            repository.saveGeneratedRoom(room);
        }

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                repository.linkExit("room-" + i + ":0", "room-" + (i + 1));
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                repository.getLinkedRoomId("room-" + i + ":0");
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Verify all links were created
        for (int i = 0; i < 10; i++) {
            assertNotNull(repository.getLinkedRoomId("room-" + i + ":0"));
        }
    }

    @Test
    @DisplayName("Should preserve current room when discarding")
    void testPreserveCurrentRoom() {
        Room current = new Room("current", "Current Room", "This is current.",
                new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(current);

        Room adjacent1 = new Room("adj-1", "Adjacent 1", "Desc",
                new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(adjacent1);

        Room unused = new Room("unused", "Unused Room", "Desc",
                new ArrayList<>(), List.of(), List.of(), List.of());
        repository.saveGeneratedRoom(unused);

        // Keep current and adjacent1, discard unused
        repository.discardUnusedRooms("current", Set.of("adj-1"));

        assertTrue(repository.exists("current"));
        assertTrue(repository.exists("adj-1"));
        assertFalse(repository.exists("unused"));
    }
}







