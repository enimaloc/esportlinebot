package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.WakfuClient;
import fr.enimaloc.esportline.api.wakfu.json.item.Item;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Optional;

public record CollectibleResource(int id, int skillId, int resourceId, int resourceIndex, int collectItemId, int resourceNextIndex, int skillLevelRequired, int simultaneousPlayer, int visualFeedbackId, int duration, int mruOrder, int xpFactor, int collectLootListId, int collectConsumableItemId, int collectGfxId, boolean displayInCraftDialog) implements Identifier {
    public Optional<Resource> resource(WakfuJSON client) {
        return client.getResource(resourceId());
    }

    public Optional<Resource> resource(WakfuClient client) {
        return resource(client.getJsonRepository());
    }

    public Optional<Item> collectItem(WakfuJSON client) {
        return client.getItem(collectItemId());
    }

    public Optional<Item> collectItem(WakfuClient client) {
        return collectItem(client.getJsonRepository());
    }
}
