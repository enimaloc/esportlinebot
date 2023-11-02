package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Arrays;
import java.util.Map;

public record ItemType(Definition definition, Map<WakfuLocale, String> title) implements Identifier {

    @Override
    public int id() {
        return definition().id();
    }

    public record Definition(int id, int parentId,
                             String[] equipmentPositions, String[] equipmentDisabledPositions,
                             boolean isRecyclable, boolean isVisibleInAnimation) {
        @Override
        public String toString() {
            return "Definition{" +
                    "id=" + id() +
                    ", parentId=" + parentId() +
                    ", equipmentPositions=" + Arrays.toString(equipmentPositions()) +
                    ", equipmentDisabledPositions=" + Arrays.toString(equipmentDisabledPositions()) +
                    ", isRecyclable=" + isRecyclable() +
                    ", isVisibleInAnimation=" + isVisibleInAnimation() +
                    '}';
        }
    }
}
