package org.bhp.heros_journey;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service for persisting newly generated items and NPCs to the library.yml file.
 * Ensures that dynamically created game content is saved for future use.
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

        // Check if item already exists
        if (libraryService.getAllItemIds().contains(item.getId())) {
            log.debug("Item already exists in library: {}", item.getId());
            return false;
        }

        try {
            // Add to in-memory library
            libraryService.getItems().add(item);
            log.info("Added new item to library: {} - {}", item.getId(), item.getName());

            // Persist to file
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

        // Check if NPC already exists
        if (libraryService.getAllNpcIds().contains(npc.getId())) {
            log.debug("NPC already exists in library: {}", npc.getId());
            return false;
        }

        try {
            // Add to in-memory library
            libraryService.getNpcs().add(npc);
            log.info("Added new NPC to library: {} - {}", npc.getId(), npc.getName());

            // Persist to file
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

    /**
     * Persists the current in-memory library state to the YAML file.
     * Uses a custom representer to maintain proper YAML formatting.
     */
    private synchronized void persistLibraryToFile() throws IOException {
        try {
            Map<String, Object> libraryMap = new LinkedHashMap<>();
            Map<String, Object> gameLibraryMap = new LinkedHashMap<>();

            // Preserve ordering and structure
            gameLibraryMap.put("npcs", libraryService.getNpcs());
            gameLibraryMap.put("items", libraryService.getItems());
            libraryMap.put("game-library", gameLibraryMap);

            // Configure YAML output for readability
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(new Representer(options), options);

            // Write to file
            Path path = Paths.get(libraryPath);
            try (FileWriter writer = new FileWriter(path.toFile())) {
                yaml.dump(libraryMap, writer);
                log.debug("Library persisted to file: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to persist library to file: {}", libraryPath, e);
            throw e;
        }
    }
}
