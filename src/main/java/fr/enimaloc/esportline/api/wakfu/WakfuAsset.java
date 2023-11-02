package fr.enimaloc.esportline.api.wakfu;

import fr.enimaloc.esportline.api.wakfu.json.Resource;
import fr.enimaloc.esportline.api.wakfu.json.item.Item;
import fr.enimaloc.esportline.api.wakfu.json.marker.Gfx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.function.Consumer;

public class WakfuAsset {
    public static final Logger LOGGER = LoggerFactory.getLogger(WakfuAsset.class);

    public Optional<byte[]> getAsset(Gfx gfx, boolean male) {
        return getAsset(gfx, male, null);
    }

    public Optional<byte[]> getAsset(Gfx gfx, boolean male, Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getAsset0(gfx, male));
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get image of "+gfx, e);
            }
            return Optional.empty();
        }
    }

    public static final String ASSET_BASE_URL = "https://vertylo.github.io/wakassets/";

    public byte[] getAsset0(Gfx gfx, boolean male) throws IOException {
        String path = "";
        if (gfx instanceof Item) {
            path = "items/";
        }
        return getAsset0(path+(male ? gfx.gfxId() : gfx.getFemaleGfxId().orElse(gfx.gfxId()))+".png");
    }

    public Optional<byte[]> getAsset(Resource resource) {
        return getAsset(resource, null);
    }

    public Optional<byte[]> getAsset(Resource resource, Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getAsset0(resource));
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get image of "+resource, e);
            }
            return Optional.empty();
        }
    }

    public byte[] getAsset0(Resource resource) throws IOException {
        return getAsset0("items/"+resource.gfxId()+".png");
    }

    public Optional<byte[]> getAsset(String path, int id) {
        return getAsset(path+"/"+id+".png");
    }

    public Optional<byte[]> getAsset(String path, int id, Consumer<IOException> throwable) {
        return getAsset(path+"/"+id+".png", throwable);
    }

    public byte[] getAsset0(String path, String id) throws IOException {
        return getAsset0(path+"/"+id+".png");
    }

    public Optional<byte[]> getAsset(String path) {
        return getAsset(path, null);
    }

    public Optional<byte[]> getAsset(String path, Consumer<IOException> throwable) {
        try {
            return Optional.ofNullable(getAsset0(path));
        } catch (IOException e) {
            if (throwable != null) {
                throwable.accept(e);
            } else {
                LOGGER.warn("Cannot get image of "+path, e);
            }
            return Optional.empty();
        }
    }

    public byte[] getAsset0(String path) throws IOException {
        URL url = URI.create(ASSET_BASE_URL + path).toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        return urlConnection.getInputStream().readAllBytes();
    }
}
