package fr.enimaloc.esportline.commands.slash.game.smash;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.IOException;
import java.sql.SQLException;

public record SmashSets(
        SmashBros origin,
        SmashBros.SmashGame game,
        String key,
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

    public record GameData(
            @JsonAlias("winner_id") int winnerId,
            @JsonAlias("loser_id") int loserId,
            @JsonAlias("winner_score") int winnerScore,
            @JsonAlias("loser_score") int loserScore,
            @JsonAlias("winner_char") String winnerCharacter,
            @JsonAlias("loser_char") String loserCharacter,
            String stage
    ) {}
}
