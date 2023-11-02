package fr.enimaloc.esportline.api.wakfu.json.recipe;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.json.item.Item;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

public record RecipeIngredient(int recipeId, int itemId, int quantity, int ingredientOrder) implements Identifier {
    @Override
    public int id() {
        return recipeId();
    }

    public Recipe recipe(WakfuJSON client) {
        return client.getRecipe(recipeId()).orElseThrow();
    }

    public RecipeResult result(WakfuJSON client) {
        return recipe(client).result(client);
    }

    public Item item(WakfuJSON client) {
        return client.getItem(itemId).orElseThrow();
    }
}
