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