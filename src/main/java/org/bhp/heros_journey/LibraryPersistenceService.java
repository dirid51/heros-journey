package org.bhp.heros_journey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for persisting newly generated items and NPCs to the library.yml file.
 * Ensures that dynamically created game content is saved for future use.
 *
 * <p>Serialization note: SnakeYAML will emit Java class-type tags (e.g.
 * {@code !!org.bhp...NpcTemplate}) when dumping POJOs directly.  Those tags
 * break {@code YamlPropertiesFactoryBean} on the next startup.  To avoid this,
 * every object is converted to a plain {@code LinkedHashMap} before serialization
 * so the output is tag-free and matches the hand-authored library.yml structure.</p>
 */
@Service
public class LibraryPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(LibraryPersistenceService.class);

    @Value("${game.library.path:src/main/resources/library.yml}")
    private String libraryPath;

    private final YamlLibraryService libraryService;

    public LibraryPersistenceService(YamlLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Adds a new item to the library and persists it to the YAML file.
     *
     * @param item the ItemTemplate to add
     * @return true if successfully added, false if already exists
     */
    public synchronized boolean addNewItem(YamlLibraryService.ItemTemplate item) {
        if (item == null || item.getId() == null || item.getId().isBlank()) {
            log.warn("Cannot add item: missing id or null");
            return false;
        }

        if (libraryService.getAllItemIds().contains(item.getId())) {
            log.debug("Item already exists in library: {}", item.getId());
            return false;
        }

        try {
            libraryService.getItems().add(item);
            log.info("Added new item to library: {} - {}", item.getId(), item.getName());
            persistLibraryToFile();
            return true;
        } catch (Exception e) {
            log.error("Failed to add new item: {}", item.getId(), e);
            return false;
        }
    }

    /**
     * Adds a new NPC to the library and persists it to the YAML file.
     *
     * @param npc the NpcTemplate to add
     * @return true if successfully added, false if already exists
     */
    public synchronized boolean addNewNpc(YamlLibraryService.NpcTemplate npc) {
        if (npc == null || npc.getId() == null || npc.getId().isBlank()) {
            log.warn("Cannot add NPC: missing id or null");
            return false;
        }

        if (libraryService.getAllNpcIds().contains(npc.getId())) {
            log.debug("NPC already exists in library: {}", npc.getId());
            return false;
        }

        try {
            libraryService.getNpcs().add(npc);
            log.info("Added new NPC to library: {} - {}", npc.getId(), npc.getName());
            persistLibraryToFile();
            return true;
        } catch (Exception e) {
            log.error("Failed to add new NPC: {}", npc.getId(), e);
            return false;
        }
    }

    /**
     * Batch adds multiple items to the library.
     *
     * @param items the ItemTemplates to add
     * @return the count of successfully added items
     */
    public int addNewItems(List<YamlLibraryService.ItemTemplate> items) {
        int added = 0;
        for (YamlLibraryService.ItemTemplate item : items) {
            if (addNewItem(item)) {
                added++;
            }
        }
        return added;
    }

    /**
     * Batch adds multiple NPCs to the library.
     *
     * @param npcs the NpcTemplates to add
     * @return the count of successfully added NPCs
     */
    public int addNewNpcs(List<YamlLibraryService.NpcTemplate> npcs) {
        int added = 0;
        for (YamlLibraryService.NpcTemplate npc : npcs) {
            if (addNewNpc(npc)) {
                added++;
            }
        }
        return added;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Persists the current in-memory library state to the YAML file.
     *
     * <p>Objects are converted to plain {@link LinkedHashMap}s before dumping so
     * that SnakeYAML never emits Java class-type tags.  The resulting YAML is
     * structurally identical to the hand-authored {@code library.yml} and can be
     * read back cleanly by {@link YamlPropertySourceFactory} /
     * {@code YamlPropertiesFactoryBean}.</p>
     */
    private synchronized void persistLibraryToFile() throws IOException {
        // Build a plain-map representation of the whole library
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> gameLibrary = new LinkedHashMap<>();

        gameLibrary.put("npcs", libraryService.getNpcs().stream()
                .map(LibraryPersistenceService::npcToMap)
                .toList());

        gameLibrary.put("items", libraryService.getItems().stream()
                .map(LibraryPersistenceService::itemToMap)
                .toList());

        root.put("game-library", gameLibrary);

        // Configure YAML for block-style, human-readable output
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml yaml = new Yaml(options);

        Path path = Paths.get(libraryPath);
        try (FileWriter writer = new FileWriter(path.toFile())) {
            yaml.dump(root, writer);
            log.debug("Library persisted to file: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to persist library to file: {}", libraryPath, e);
            throw e;
        }
    }

    /**
     * Converts an {@link YamlLibraryService.NpcTemplate} to a plain {@link LinkedHashMap}.
     * Field order matches the hand-authored library.yml for readability.
     */
    private static Map<String, Object> npcToMap(YamlLibraryService.NpcTemplate npc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", npc.getId());
        map.put("name", npc.getName());
        map.put("goal", npc.getGoal());
        map.put("description", npc.getDescription());
        return map;
    }

    /**
     * Converts an {@link YamlLibraryService.ItemTemplate} to a plain {@link LinkedHashMap}.
     * Field order matches the hand-authored library.yml for readability.
     */
    private static Map<String, Object> itemToMap(YamlLibraryService.ItemTemplate item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        return map;
    }
}