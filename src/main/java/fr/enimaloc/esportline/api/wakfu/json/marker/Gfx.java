package fr.enimaloc.esportline.api.wakfu.json.marker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.enimaloc.esportline.api.wakfu.WakfuClient;
import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;

import java.util.Optional;
import java.util.OptionalInt;

public interface Gfx {
    int gfxId();

    @JsonIgnore
    default OptionalInt getFemaleGfxId() {
        return OptionalInt.empty();
    }

    default byte[] getGfx(WakfuJSON client) {
        return getGfx(client.getBaseClient());
    }

    default Optional<byte[]> getFemaleGfx(WakfuJSON client) {
        return getFemaleGfx(client.getBaseClient());
    }

    default byte[] getGfx(WakfuClient client) {
        return client.getAssetsRepository().getAsset(this, true).orElseThrow();
    }

    default Optional<byte[]> getFemaleGfx(WakfuClient client) {
        return getFemaleGfxId().isPresent() ? client.getAssetsRepository().getAsset(this, false) : Optional.empty();
    }
}
