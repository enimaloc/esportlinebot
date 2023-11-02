package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Map;

public record Action(Definition definition, Map<WakfuLocale, String> description) implements Identifier {
    public Action {
        if(definition.id() == 1020) { // Hardcoded value
            description = Map.of(
                    WakfuLocale.FRENCH, "Renvoie 10% des dégâts",
                    WakfuLocale.ENGLISH, "Reflects 10% of damage",
                    WakfuLocale.SPANISH, "Devuelve 10% de daño",
                    WakfuLocale.PORTUGUESE, "Reenvia 10% dos dano"
            );
        }
    }

    @Override
    public int id() {
        return definition().id();
    }

    public record Definition(int id, String effect) {}
}
