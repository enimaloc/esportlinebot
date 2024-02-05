package fr.enimaloc.esportline.api.smash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public record SmashPlayer(
        SmashRegistry origin,
        SmashRegistry.SmashGame game,
        boolean incomplete,
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmashPlayer that = (SmashPlayer) o;

        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return playerId;
    }

    @Override
    public String toString() {
        return "SmashPlayer{" +
                "incomplete=" + incomplete +
                ", playerId=" + playerId +
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

    public record Placings(SmashRegistry origin, SmashRegistry.SmashGame game, int playerId, String key, int placing, int seed, boolean dq) {
        public Placings(SmashRegistry origin, SmashRegistry.SmashGame game, int playerId, String json) throws JsonProcessingException {
            this(origin, game, playerId, new JsonMapper().readTree(json));
        }

        public Placings(SmashRegistry origin, SmashRegistry.SmashGame game, int playerId, JsonNode json) {
            this(origin, game, playerId, json.get("key").asText(), json.get("placing").asInt(), json.get("seed").asInt(), json.get("dq").asBoolean());
        }

        public SmashTournament getTournament() throws SQLException, IOException {
            return origin.getTournamentTable(game).getTournamentByKey(key);
        }

        public List<SmashSets> getSets() throws SQLException, IOException {
            return origin.getSetTable(game).getSetsForTournamentAndPlayer(key, String.valueOf(playerId));
        }
    }

    public record Character(String name, int usage) {
    }

    public SmashPlayer complete() throws SQLException, JsonProcessingException {
        if (!incomplete) return this;
        return origin.getPlayerTable(game).getPlayerById(String.valueOf(playerId));
    }

//    public List<SmashTournament> getTournaments() throws SQLException, IOException {
//        List<SmashTournament> tournaments = new ArrayList<>();
//        for (Placings placing : placings) {
//            tournaments.add(origin.getTournament(game, placing.key()));
//        }
//        return tournaments;
//    }
//
//    public List<SmashSets> getSets() throws SQLException, IOException {
//        return origin.getSetsForPlayer(game, String.valueOf(playerId));
//    }
}