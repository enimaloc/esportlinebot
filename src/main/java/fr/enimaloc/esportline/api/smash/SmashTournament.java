package fr.enimaloc.esportline.api.smash;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public record SmashTournament(
        SmashBros origin,
        SmashBros.SmashGame game,
        boolean incomplete,
        String key,
        String cleanedName,
        String source,
        String tournamentName,
        String tournamentEvent,
        int season,
        char rank,
        long start,
        long end,
        Location location,
        int entrants,
        Placing[] placings,
        String losses,
        String bracketTypes,
        boolean online,
        GPSLocation gpsLocation
) {
    public record Placing(SmashBros origin, SmashBros.SmashGame game, String playerId, int placings) {
        public SmashPlayer getPlayer() throws SQLException, IOException {
            return origin.getPlayerTable(game).getPlayerById(playerId);
        }
    }

    public List<SmashSets> getSets() throws SQLException, IOException {
        return origin.getSetTable(game).getSetsForTournament(key);
    }

    public SmashTournament complete() throws SQLException, JsonProcessingException {
        if (!incomplete) return this;
        return origin.getTournamentTable(game).getTournamentByKey(key);
    }
}
