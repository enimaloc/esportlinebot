package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.WakfuClient;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Gfx;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public record Resource(Definition definition, Map<WakfuLocale, String> title) implements Identifier, Gfx {
    @Override
    public int id() {
        return definition().id();
    }

    @Override
    public int gfxId() {
        return definition().iconGfxId();
    }

    public record Definition(int id, int resourceType,
                             boolean isBlocking, boolean usableByHeroes,
                             int idealRainRangeMin, int idealRainRangeMax, int idealRain,
                             int idealTemperatureRangeMin, int idealTemperatureRangeMax,
                             int iconGfxId, int lastEvolutionStep) {
        public ResourceType resourceType(WakfuJSON client) {
            return client.getResourceType(resourceType()).orElseThrow();
        }

        public ResourceType resourceType(WakfuClient client) {
            return resourceType(client.getJsonRepository());
        }

        public Optional<CollectibleResource[]> getCollectibleResource(WakfuJSON client) {
            return Optional.of(client.getCollectibleResources()
                    .stream()
                    .flatMap(Arrays::stream)
                    .filter(harvestLoot -> harvestLoot.resourceId() == id())
                    .toArray(CollectibleResource[]::new));
        }

        public Optional<CollectibleResource[]> getCollectibleResource(WakfuClient client) {
            return getCollectibleResource(client.getJsonRepository());
        }
    }
}
