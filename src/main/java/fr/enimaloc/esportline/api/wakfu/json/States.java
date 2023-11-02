package fr.enimaloc.esportline.api.wakfu.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record States(Definition definition, Map<WakfuLocale, String> title, Map<WakfuLocale, String> description) implements Identifier {

    @Override
    public int id() {
        return definition().id();
    }

    public Map<WakfuLocale, String> title() {
        if (title == null) {
            return Map.of();
        }
        return title;
    }

    public Map<WakfuLocale, String> description() {
        if (description == null) {
            return Map.of();
        }
        return description;
    }

    public record Definition(int id) {}
}
