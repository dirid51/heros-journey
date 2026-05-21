package org.bhp.heros_journey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class SaveGameService {

    private static final Logger log = LoggerFactory.getLogger(SaveGameService.class);
    private static final Path SAVE_DIR = Paths.get("saves");

    public void save(String sessionId, GameState state, RoomRepository repo) {
        try {
            Files.createDirectories(SAVE_DIR);
            Path file = SAVE_DIR.resolve(sessionId + ".sav");
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)))) {
                oos.writeObject(state);
                oos.writeObject(repo);
            }
        } catch (IOException e) {
            log.error("Failed to save game for session {}", sessionId, e);
        }
    }

    public Optional<SaveData> load(String saveCode) {
        Path file = SAVE_DIR.resolve(saveCode + ".sav");
        if (!Files.exists(file)) return Optional.empty();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            GameState state = (GameState) ois.readObject();
            RoomRepository repo = (RoomRepository) ois.readObject();
            return Optional.of(new SaveData(state, repo));
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load save {}", saveCode, e);
            return Optional.empty();
        }
    }

    public record SaveData(GameState state, RoomRepository repo) {
    }
}