package fr.enimaloc.esportline.api.wakfu.json;

import fr.enimaloc.esportline.api.wakfu.WakfuClient;

public record GamedataConfig(String version) {
    public WakfuJSON newClient(WakfuClient baseClient) {
        return new WakfuJSON(baseClient, version);
    }
}
