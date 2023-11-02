package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Map;

public record ResourceType(Definition definition, Map<WakfuLocale, String> title) implements Identifier {

    @Override
    public int id() {
        return definition().id();
    }

    public record Definition(int id, boolean affectWakfu) {}
}
