package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.global.GraphicParameters;
import fr.enimaloc.esportline.api.wakfu.json.global.Rarity;

import java.util.Map;

public record JobItem(Definition definition, Map<WakfuLocale, String> title, Map<WakfuLocale, String> description) implements Item {
    @Override
    public int id() {
        return definition().id();
    }

    @Override
    public Rarity rarity() {
        return definition().rarity();
    }

    @Override
    public int level() {
        return definition().level();
    }

    @Override
    public int gfxId() {
        return definition().graphicParameters().gfxId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobItem jobItem = (JobItem) o;

        if (id() != jobItem.id()) return false;
        if (level() != jobItem.level()) return false;
        if (!title.equals(jobItem.title)) return false;
        if (!description.equals(jobItem.description)) return false;
        if (!definition.equals(jobItem.definition)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = definition.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + level();
        result = 31 * result + id();
        return result;
    }

    public record Definition(int id, int level, Rarity rarity, int itemTypeId, GraphicParameters graphicParameters) {
        public ItemType fetchItemType(WakfuJSON client) {
            return client.getItemType(itemTypeId()).orElseThrow();
        }
    }
}
