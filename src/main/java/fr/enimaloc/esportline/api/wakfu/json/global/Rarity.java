package fr.enimaloc.esportline.api.wakfu.json.global;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.awt.*;
import java.util.Arrays;

public enum Rarity {
    COMMON(0, new Color(0x988f81)),
    UNCOMMON(1, new Color(0xe8e6e3)),
    RARE(2, new Color(0x31ff31)),
    MYTHIC(3, new Color(0xffae1a)),
    LEGENDARY(4, new Color(0xffff1a)),
    RELIC(5, new Color(0xff1aff)),
    EPIC(6, new Color(0xff63b1)),
    SOUVENIR(7, new Color(52991));

    private final int id;
    private final Color color;

    Rarity(int id, Color color) {
        this.id = id;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    @JsonCreator
    public static Rarity fromId(int id) {
        return Arrays.stream(values())
                .filter(rarity -> rarity.id == id)
                .findFirst()
                .orElseThrow();
    }
}
