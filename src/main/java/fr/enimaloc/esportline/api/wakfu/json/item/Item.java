package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.global.Rarity;
import fr.enimaloc.esportline.api.wakfu.json.marker.Gfx;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;
import fr.enimaloc.esportline.api.wakfu.json.recipe.Recipe;
import fr.enimaloc.esportline.api.wakfu.json.recipe.RecipeIngredient;
import fr.enimaloc.esportline.api.wakfu.json.recipe.RecipeResult;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public interface Item extends Identifier, Gfx {
    Map<WakfuLocale, String> title();
    Map<WakfuLocale, String> description();
    Rarity rarity();

    default RecipeIngredient[] recipesRelated(WakfuJSON client) {
        return client.getRecipeIngredients()
                .stream()
                .flatMap(Arrays::stream)
                .filter(recipeIngredient -> recipeIngredient.itemId() == id())
                .toArray(RecipeIngredient[]::new);
    }

    default Optional<RecipeResult> recipeResult(WakfuJSON client) {
        return Arrays.stream(client.getRecipeResults().orElseThrow())
                .filter(recipeResult -> recipeResult.productedItemId() == id())
                .findFirst();
    }

    default Optional<Recipe> recipe(WakfuJSON client) {
        return recipeResult(client).map(recipeResult -> recipeResult.recipe(client));
    }

    int level();
}
