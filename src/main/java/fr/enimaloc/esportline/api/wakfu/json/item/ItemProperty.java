package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

public record ItemProperty(int id, String name, String description) implements Identifier {
}
