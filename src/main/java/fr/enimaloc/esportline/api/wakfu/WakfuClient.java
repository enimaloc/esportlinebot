package fr.enimaloc.esportline.api.wakfu;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.rss.WakfuRSS;

import java.io.IOException;
import java.net.URI;

public class WakfuClient {
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private final WakfuJSON jsonRepository;
    private final WakfuAsset assetsRepository;

    public WakfuClient() {
        this((String) null);
    }

    public WakfuClient(String version) {
        this.jsonRepository = new WakfuJSON(this, version);
        this.assetsRepository = new WakfuAsset();
    }

    public WakfuClient(WakfuJSON jsonRepository) {
        this.jsonRepository = jsonRepository;
        this.assetsRepository = new WakfuAsset();
    }

    public WakfuJSON getJsonRepository() {
        return jsonRepository;
    }

    public WakfuAsset getAssetsRepository() {
        return assetsRepository;
    }

    public WakfuRSS getRssFlux(WakfuLocale locale) throws IOException {
        return XML_MAPPER.readValue(URI.create("https://www.wakfu.com/%s/mmorpg/rss.xml".formatted(locale.code())).toURL(),
                WakfuRSS.class);
    }
}
