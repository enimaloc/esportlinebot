package fr.enimaloc.esportline.api.wakfu.json.global;

import fr.enimaloc.esportline.api.wakfu.json.marker.Gfx;

import java.util.OptionalInt;

public record GraphicParameters(int gfxId, int femaleGfxId) implements Gfx {
    public GraphicParameters(int gfxId) {
        this(gfxId, -1);
    }

    @Override
    public OptionalInt getFemaleGfxId() {
        return femaleGfxId == -1 ? OptionalInt.empty() : OptionalInt.of(femaleGfxId);
    }
}
