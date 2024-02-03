package fr.enimaloc.esportline.api.smash;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public record SmashTournament(
        SmashBros origin,
        SmashBros.SmashGame game,
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
            return origin.getPlayer(game, playerId);
        }
    }

    public List<SmashSets> getSets() throws SQLException, IOException {
        return origin.getSetsForTournament(game, key);
    }
}
