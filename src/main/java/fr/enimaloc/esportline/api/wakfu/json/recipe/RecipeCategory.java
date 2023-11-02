package fr.enimaloc.esportline.api.wakfu.json.recipe;

import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Map;

public record RecipeCategory(Definition definition, Map<WakfuLocale, String> title) implements Identifier {

    @Override
    public int id() {
        return definition().id();
    }

    public record Definition(int id,
                             boolean isArchive, boolean isNoCraft, boolean isHidden, boolean isInnate,
                             int xpFactor) {}
}
