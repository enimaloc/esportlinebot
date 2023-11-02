package fr.enimaloc.esportline.api.wakfu.json.recipe;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.json.item.Item;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

public record RecipeResult(int recipeId, int productedItemId, int productOrder, int productedItemQuantity) implements
        Identifier {
    @Override
    public int id() {
        return recipeId();
    }

    public Recipe recipe(WakfuJSON client) {
        return client.getRecipe(recipeId()).orElseThrow();
    }

    public RecipeIngredient[] ingredients(WakfuJSON client) {
        return recipe(client).ingredients(client);
    }

    public Item item(WakfuJSON wakfuClient) {
        return wakfuClient.getItem(productedItemId).orElseThrow();
    }
}
