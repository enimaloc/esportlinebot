package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

public record HarvestLoot(int id, int itemId, int quantity, int requiredProspection, double dropRate, int listId, int quantityPerItem, int quantityMin, int quantityMax, int maxRoll, boolean itemIsLootList) implements Identifier {
}
