package fr.enimaloc.esportline.commands.slash.game.smash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.Arrays;

public record SmashPlayer(
        SmashBros.SmashGame game,
        int playerId,
        String tag,
        String[] allTags,
        String[] prefixes,
        Social[] social,
        Location location,
        Placings[] placings,
        Character[] characters,
        String alias
) {

    @Override
    public String toString() {
        return "SmashPlayer{" +
                "playerId=" + playerId +
                ", tag='" + tag + '\'' +
                ", allTags=" + Arrays.toString(allTags) +
                ", prefixes=" + Arrays.toString(prefixes) +
                ", social=" + Arrays.toString(social) +
                ", location=" + location +
                ", placings=" + Arrays.toString(placings) +
                ", characters=" + Arrays.toString(characters) +
                ", alias='" + alias + '\'' +
                '}';
    }

    public record Social(String type, String[] values) {
        @Override
        public String toString() {
            return "Social{" +
                    "type='" + type + '\'' +
                    ", values=" + Arrays.toString(values) +
                    '}';
        }
    }

    public record Placings(String key, int placing, int seed, boolean dq) {
        public Placings(String json) throws JsonProcessingException {
            this(new JsonMapper().readTree(json));
        }

        public Placings(JsonNode json) {
            this(json.get("key").asText(), json.get("placing").asInt(), json.get("seed").asInt(), json.get("dq").asBoolean());
        }
    }

    public record Character(String name, int usage) {
    }
}
