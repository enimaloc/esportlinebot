package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Arrays;

public record Blueprint(int blueprintId, int[] recipeId) implements Identifier {
    @Override
    public int id() {
        return blueprintId();
    }

    @Override
    public String toString() {
        return "Blueprint{" +
                "blueprintId=" + blueprintId +
                ", recipeId=" + Arrays.toString(recipeId) +
                '}';
    }
}
