package org.bhp.heros_journey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Manages a persistent corpus of generated room descriptions and skill opportunity
 * patterns. These are injected into future room generation prompts as atmospheric
 * style references, gradually improving narrative variety over time without
 * locking in full room structures.
 *
 * <p>The corpus is bounded to prevent unbounded file growth. When either list
 * reaches its cap, a random entry is evicted to make room for the new one,
 * giving older entries a chance to age out naturally.</p>
 */
@Service
public class RoomInspirationService {

    private static final Logger log = LoggerFactory.getLogger(RoomInspirationService.class);

    private static final int MAX_ROOM_ENTRIES = 200;
    private static final int MAX_SKILL_ENTRIES = 150;

    @Value("${game.inspirations.path:src/main/resources/inspirations.json}")
    private String inspirationsPath;

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // Plain synchronized lists rather than CopyOnWriteArrayList because we do
    // frequent random-index removal, which COW handles poorly.
    private final List<RoomInspiration> rooms = Collections.synchronizedList(new ArrayList<>());
    private final List<String> skillPatterns = Collections.synchronizedList(new ArrayList<>());
    private final List<String> skillNames = Collections.synchronizedList(new ArrayList<>());

    public RoomInspirationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        Path path = Paths.get(inspirationsPath);
        if (!Files.exists(path)) {
            log.info("No inspirations file found at {}. Starting with empty corpus.", path.toAbsolutePath());
            return;
        }
        try {
            InspirationCorpus corpus = objectMapper.readValue(path.toFile(), InspirationCorpus.class);
            if (corpus.rooms() != null) rooms.addAll(corpus.rooms());
            if (corpus.skillPatterns() != null) skillPatterns.addAll(corpus.skillPatterns());
            if (corpus.skillNames() != null) skillNames.addAll(corpus.skillNames());
            log.info("Loaded {} skill names from corpus.", skillNames.size());
            log.info("Loaded {} room inspirations and {} skill patterns from corpus.",
                    rooms.size(), skillPatterns.size());
        } catch (IOException e) {
            log.error("Failed to load inspirations file — starting with empty corpus.", e);
        }
    }

    /**
     * Adds a room title and the first sentence of its description to the corpus.
     * Only the first sentence is stored to keep prompt injection concise.
     * Duplicate titles are silently ignored.
     */
    public void addRoom(String title, String description) {
        if (title == null || title.isBlank() || description == null || description.isBlank()) return;

        synchronized (rooms) {
            boolean duplicate = rooms.stream().anyMatch(r -> r.title().equalsIgnoreCase(title));
            if (duplicate) return;

            if (rooms.size() >= MAX_ROOM_ENTRIES) {
                rooms.remove(random.nextInt(rooms.size()));
            }
            rooms.add(new RoomInspiration(title, firstSentence(description)));
        }
        saveAsync();
    }

    /**
     * Adds skill opportunity patterns from a generated room.
     * Case-insensitive duplicates are skipped.
     */
    public void addSkillOpportunities(List<String> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) return;

        boolean changed = false;
        synchronized (skillPatterns) {
            for (String pattern : opportunities) {
                if (pattern == null || pattern.isBlank()) continue;
                String normalized = pattern.trim();
                boolean exists = skillPatterns.stream().anyMatch(p -> p.equalsIgnoreCase(normalized));
                if (!exists) {
                    if (skillPatterns.size() >= MAX_SKILL_ENTRIES) {
                        skillPatterns.remove(random.nextInt(skillPatterns.size()));
                    }
                    skillPatterns.add(normalized);
                    changed = true;
                }
            }
        }
        if (changed) saveAsync();
    }

    /**
     * Returns a random sample of room inspirations for prompt injection.
     */
    public List<RoomInspiration> sampleRooms(int count) {
        synchronized (rooms) {
            if (rooms.isEmpty()) return List.of();
            List<RoomInspiration> copy = new ArrayList<>(rooms);
            Collections.shuffle(copy, random);
            return List.copyOf(copy.subList(0, Math.min(count, copy.size())));
        }
    }

    /**
     * Returns a random sample of skill opportunity patterns for prompt injection.
     */
    public List<String> sampleSkillPatterns(int count) {
        synchronized (skillPatterns) {
            if (skillPatterns.isEmpty()) return List.of();
            List<String> copy = new ArrayList<>(skillPatterns);
            Collections.shuffle(copy, random);
            return List.copyOf(copy.subList(0, Math.min(count, copy.size())));
        }
    }

    // CompletableFuture.runAsync avoids the Spring self-invocation problem
    // that would prevent @Async from working inside the same bean.
    private void saveAsync() {
        CompletableFuture.runAsync(this::persist);
    }

    private void persist() {
        InspirationCorpus corpus;
        synchronized (rooms) {
            synchronized (skillPatterns) {
                synchronized (skillNames) {
                    corpus = new InspirationCorpus(
                            new ArrayList<>(rooms),
                            new ArrayList<>(skillPatterns),
                            new ArrayList<>(skillNames)
                    );
                }
            }
        }
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Paths.get(inspirationsPath).toFile(), corpus);
            log.debug("Inspiration corpus persisted ({} rooms, {} skill patterns).",
                    corpus.rooms().size(), corpus.skillPatterns().size());
        } catch (IOException e) {
            log.error("Failed to persist inspiration corpus.", e);
        }
    }

    private String firstSentence(String text) {
        // Prefer ". " over lone "." to avoid splitting on abbreviations mid-sentence
        int end = text.indexOf(". ");
        if (end == -1) end = text.lastIndexOf('.');
        return (end == -1) ? text : text.substring(0, end + 1);
    }

    public record RoomInspiration(String title, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InspirationCorpus(
            List<RoomInspiration> rooms,
            List<String> skillPatterns,
            List<String> skillNames
    ) {}

    public void addSkillName(String name) {
        if (name == null || name.isBlank()) return;
        synchronized (skillNames) {
            boolean exists = skillNames.stream().anyMatch(n -> n.equalsIgnoreCase(name));
            if (exists) return;
            if (skillNames.size() >= 300) {
                skillNames.remove(random.nextInt(skillNames.size()));
            }
            skillNames.add(name.trim());
        }
        saveAsync();
    }

    public List<String> sampleSkillNames(int count) {
        synchronized (skillNames) {
            if (skillNames.isEmpty()) return List.of();
            List<String> copy = new ArrayList<>(skillNames);
            Collections.shuffle(copy, random);
            return List.copyOf(copy.subList(0, Math.min(count, copy.size())));
        }
    }
}