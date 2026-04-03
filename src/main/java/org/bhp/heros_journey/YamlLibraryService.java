package org.bhp.heros_journey;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
@ConfigurationProperties(prefix = "game-library")
@PropertySource(value = "classpath:library.yml", factory = org.bhp.heros_journey.YamlPropertySourceFactory.class)
public class YamlLibraryService {

    private List<NpcTemplate> npcs;
    private List<ItemTemplate> items;

    public List<String> getAllNpcIds() {
        return npcs == null ? List.of() : npcs.stream().map(NpcTemplate::getId).collect(Collectors.toList());
    }

    public List<String> getAllItemIds() {
        return items == null ? List.of() : items.stream().map(ItemTemplate::getId).collect(Collectors.toList());
    }

    public ItemTemplate getItemById(String id) {
        return items.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(this.getRandomItem());
    }

    private ItemTemplate getRandomItem() {
        // return a random ItemTemplate from the list
        return items.get((int) (Math.random() * items.size()));
    }

    public NpcTemplate getNpcById(String id) {
        return npcs.stream().filter(npc -> npc.getId().equals(id)).findFirst().orElse(this.getRandomNpc());
    }

    private NpcTemplate getRandomNpc() {
        return npcs.get((int) (Math.random() * npcs.size()));
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