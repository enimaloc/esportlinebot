package fr.enimaloc.esportline.api.wakfu.json.recipe;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Arrays;

public record Recipe(int id, int categoryId, int level, int xpRatio, boolean isUpgrade, int upgradeItemId) implements
        Identifier {
    public RecipeResult result(WakfuJSON client) {
        return client.getRecipeResult(id).orElseThrow();
    }

    public RecipeIngredient[] ingredients(WakfuJSON client) {
        return client.getRecipeIngredients()
                .stream()
                .flatMap(Arrays::stream)
                .filter(recipeIngredient -> recipeIngredient.recipeId() == id)
                .toArray(RecipeIngredient[]::new);
    }

    public RecipeCategory category(WakfuJSON wakfuClient) {
        return wakfuClient.getRecipeCategory(categoryId).orElseThrow();
    }
}
