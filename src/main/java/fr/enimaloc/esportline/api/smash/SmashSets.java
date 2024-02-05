package fr.enimaloc.esportline.api.smash;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.IOException;
import java.sql.SQLException;

public record SmashSets(
        SmashBros origin,
        SmashBros.SmashGame game,
        boolean incomplete, String key,
        String tournamentKey,
        String winnerId,
        String player1Id,
        String player2Id,
        int player1Score,
        int player2Score,
        String[] locationNames,
        String bracketName,
        String bracketOrder,
        String setOrder,
        int bestOf,
        GameData[] gameData
) {
    public SmashTournament getTournament() throws SQLException, IOException {
        return origin.getTournamentTable(game).getTournamentByKey(tournamentKey);
    }

    public SmashPlayer getWinner() throws SQLException, IOException {
        return origin.getPlayerTable(game).getPlayersByIdOrTag(winnerId).get(0);
    }

    public SmashPlayer getPlayer1() throws SQLException, IOException {
        return origin.getPlayerTable(game).getPlayersByIdOrTag(player1Id).get(0);
    }

    public SmashPlayer getPlayer2() throws SQLException, IOException {
        return origin.getPlayerTable(game).getPlayersByIdOrTag(player2Id).get(0);
    }

    public SmashPlayer getLooser() throws SQLException, IOException {
        if (player1Score > player2Score) {
            return getPlayer2();
        } else {
            return getPlayer1();
        }
    }

    public record GameData(
            @JsonAlias("winner_id") String winnerId,
            @JsonAlias("loser_id") String loserId,
            @JsonAlias("winner_score") String winnerScore,
            @JsonAlias("loser_score") String loserScore,
            @JsonAlias("winner_char") String winnerCharacter,
            @JsonAlias("loser_char") String loserCharacter,
            String stage
    ) {}
}
