package org.bhp.heros_journey;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Data
@ConfigurationProperties(prefix = "game-library")
@PropertySource(value = "classpath:library.yml", factory = org.bhp.heros_journey.YamlPropertySourceFactory.class)
public class YamlLibraryService {

    private static final Logger log = LoggerFactory.getLogger(YamlLibraryService.class);

    private List<NpcTemplate> npcs;
    private List<ItemTemplate> items;
    private final Random random = new Random();

    public List<String> getAllNpcIds() {
        return npcs == null ? List.of() : npcs.stream().map(NpcTemplate::getId).collect(Collectors.toList());
    }

    public List<String> getAllItemIds() {
        return items == null ? List.of() : items.stream().map(ItemTemplate::getId).collect(Collectors.toList());
    }

    public ItemTemplate getItemById(String id) {
        var item = items.stream().filter(itemTemplate -> itemTemplate.getId().equals(id)).findFirst();
        if (item.isEmpty()) {
            log.warn("AI generated invalid item ID: {}. Returning random item as fallback.", id);
            return this.getRandomItem();
        }
        return item.get();
    }

    private ItemTemplate getRandomItem() {
        // return a random ItemTemplate from the list
        return items.get(random.nextInt(items.size()));
    }

    public NpcTemplate getNpcById(String id) {
        var npc = npcs.stream().filter(npcTemplate -> npcTemplate.getId().equals(id)).findFirst();
        if (npc.isEmpty()) {
            log.warn("AI generated invalid NPC ID: {}. Returning random NPC as fallback.", id);
            return this.getRandomNpc();
        }
        return npc.get();
    }

    private NpcTemplate getRandomNpc() {
        return npcs.get(random.nextInt(npcs.size()));
    }

    @Data
    public static class NpcTemplate {
        private String id;
        private String name;
        private String goal;
        private String description;
    }

    @Data
    public static class ItemTemplate {
        private String id;
        private String name;
        private String description;
    }
}