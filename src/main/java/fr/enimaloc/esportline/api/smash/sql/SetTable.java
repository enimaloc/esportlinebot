package fr.enimaloc.esportline.api.smash.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import fr.enimaloc.esportline.api.smash.SmashRegistry;
import fr.enimaloc.esportline.api.smash.SmashSets;
import fr.enimaloc.esportline.api.sql.Column;
import fr.enimaloc.esportline.api.sql.Table;
import fr.enimaloc.esportline.api.sql.WhereStatement;
import fr.enimaloc.esportline.utils.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetTable extends Table {
    private static final int LOG_DETAILS_LEVEL = System.getenv("SMASH_LOG_DETAILS_LEVEL") == null
            ? 0
            : Integer.parseInt(System.getenv("SMASH_LOG_DETAILS_LEVEL").replaceFirst("0x", ""), System.getenv("SMASH_LOG_DETAILS_LEVEL").startsWith("0x") ? 16 : 10);
    public static final Logger LOGGER = LoggerFactory.getLogger(SetTable.class);

    public static final Column<String> COLUMN_KEY = new Column("key", String.class);
    public static final Column<String> COLUMN_TOURNAMENT_KEY = new Column("tournament_key", String.class);
    public static final Column<String> COLUMN_WINNER_ID = new Column("winner_id", String.class);
    public static final Column<String> COLUMN_P1_ID = new Column("p1_id", String.class);
    public static final Column<String> COLUMN_P2_ID = new Column("p2_id", String.class);
    public static final Column<Integer> COLUMN_P1_SCORE = new Column("p1_score", int.class);
    public static final Column<Integer> COLUMN_P2_SCORE = new Column("p2_score", int.class);
    public static final Column<String[]> COLUMN_LOCATION_NAMES = new Column("location_names", String[].class);
    public static final Column<String> COLUMN_BRACKET_NAME = new Column("bracket_name", String.class);
    public static final Column<String> COLUMN_BRACKET_ORDER = new Column("bracket_order", String.class);
    public static final Column<String> COLUMN_SET_ORDER = new Column("set_order", String.class);
    public static final Column<Integer> COLUMN_BEST_OF = new Column("set_order", int.class);
    public static final Column<SmashSets.GameData> COLUMN_GAME_DATA = new Column("game_data", SmashSets.GameData.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    private final SmashRegistry origin;
    private final SmashRegistry.SmashGame game;

    public SetTable(SmashRegistry origin, SmashRegistry.SmashGame game) throws SQLException, IOException {
        super(origin.getConnection(game), "sets");
        this.origin = origin;
        this.game = game;
    }

    public List<SmashSets> getSets() throws SQLException, JsonProcessingException {
        List<SmashSets> sets = new ArrayList<>();
        try (ResultSet set = select()) {
            while (set.next()) {
                sets.add(buildSet(set));
            }
        }
        return sets;
    }

    public SmashSets getSetByKey(String key) throws SQLException, JsonProcessingException {
        try (ResultSet set = select(WhereStatement.of(COLUMN_KEY).eq(key))) {
            if (set.next()) {
                return buildSet(set);
            }
        }
        throw new SQLException("Set not found");
    }

    public List<SmashSets> getSetByPlayer(String playerId) throws SQLException, JsonProcessingException {
        List<SmashSets> sets = new ArrayList<>();
        try (ResultSet set = select(WhereStatement.of(COLUMN_P1_ID).eq(playerId)
                .or(WhereStatement.of(COLUMN_P2_ID).eq(playerId)))) {
            while (set.next()) {
                sets.add(buildSet(set));
            }
        }
        return sets;
    }

    public List<SmashSets> getSetsForTournament(String key) throws SQLException, JsonProcessingException {
        List<SmashSets> sets = new ArrayList<>();
        try (ResultSet set = select(WhereStatement.of(COLUMN_TOURNAMENT_KEY).eq(key))) {
            while (set.next()) {
                sets.add(buildSet(set));
            }
        }
        return sets;
    }

    public List<SmashSets> getSetsForTournamentAndPlayer(String key, String playerId) throws SQLException, JsonProcessingException {
        List<SmashSets> sets = new ArrayList<>();
        try (ResultSet set = select(WhereStatement.of(COLUMN_TOURNAMENT_KEY).eq(key)
                .and(WhereStatement.of(COLUMN_P1_ID).eq(playerId)
                        .or(WhereStatement.of(COLUMN_P2_ID).eq(playerId))))) {
            while (set.next()) {
                sets.add(buildSet(set));
            }
        }
        return sets;
    }

    private SmashSets buildSet(ResultSet set) throws SQLException, JsonProcessingException {
        if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
            LOGGER.trace("Building sets {}", set.getString("key"));
        }

        boolean incomplete = false;
        String key = null;
        String tournamentKey = null;
        String winnerId = null;
        String player1Id = null;
        String player2Id = null;
        int player1Score = 0;
        int player2Score = 0;
        String[] locationNames = null;
        String bracketName = null;
        String bracketOrder = null;
        String setOrder = null;
        int bestOf = 0;
        SmashSets.GameData[] gameData = null;

        // region Key
        if (SqlUtils.hasColumn(set, "key")) {
            key = set.getString("key");
        } else {
            incomplete = true;
        }
        // endregion
        // region TournamentKey
        if (SqlUtils.hasColumn(set, "tournament_key")) {
            tournamentKey = set.getString("tournament_key");
        } else {
            incomplete = true;
        }
        // endregion
        // region WinnerId
        if (SqlUtils.hasColumn(set, "winner_id")) {
            winnerId = set.getString("winner_id");
        } else {
            incomplete = true;
        }
        // endregion
        // region Player1Id
        if (SqlUtils.hasColumn(set, "p1_id")) {
            player1Id = set.getString("p1_id");
        } else {
            incomplete = true;
        }
        // endregion
        // region Player2Id
        if (SqlUtils.hasColumn(set, "p2_id")) {
            player2Id = set.getString("p2_id");
        } else {
            incomplete = true;
        }
        // endregion
        // region Player1Score
        if (SqlUtils.hasColumn(set, "p1_score")) {
            player1Score = set.getInt("p1_score");
        } else {
            incomplete = true;
        }
        // endregion
        // region Player2Score
        if (SqlUtils.hasColumn(set, "p2_score")) {
            player2Score = set.getInt("p2_score");
        } else {
            incomplete = true;
        }
        // endregion
        // region LocationNames
        if (SqlUtils.hasColumn(set, "location_names")) {
            JsonNode locationNamesNode = MAPPER.readTree(set.getString("location_names"));
            locationNames = new String[locationNamesNode.size()];
            for (int i = 0; i < locationNamesNode.size(); i++) {
                locationNames[i] = locationNamesNode.get(i).asText();
            }
        } else {
            incomplete = true;
        }
        // endregion
        // region BracketName
        if (SqlUtils.hasColumn(set, "bracket_name")) {
            bracketName = set.getString("bracket_name");
        } else {
            incomplete = true;
        }
        // endregion
        // region BracketOrder
        if (SqlUtils.hasColumn(set, "bracket_order")) {
            bracketOrder = set.getString("bracket_order");
        } else {
            incomplete = true;
        }
        // endregion
        // region SetOrder
        if (SqlUtils.hasColumn(set, "set_order")) {
            setOrder = set.getString("set_order");
        } else {
            incomplete = true;
        }
        // endregion
        // region BestOf
        if (SqlUtils.hasColumn(set, "best_of")) {
            bestOf = set.getInt("best_of");
        } else {
            incomplete = true;
        }
        // endregion
        // region GameData
        if (SqlUtils.hasColumn(set, "game_data")) {
            String content = set.getString("game_data");
            if (!content.equals("{}")) {
                gameData = MAPPER.readValue(content, SmashSets.GameData[].class);
            } else {
                gameData = new SmashSets.GameData[0];
            }
        } else {
            incomplete = true;
        }
        // endregion

        SmashSets smashSets = new SmashSets(
                origin,
                game,
                incomplete,
                key,
                tournamentKey,
                winnerId,
                player1Id,
                player2Id,
                player1Score,
                player2Score,
                locationNames,
                bracketName,
                bracketOrder,
                setOrder,
                bestOf,
                gameData
        );
        if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
            LOGGER.trace("Sets {} built", key);
        } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
            LOGGER.trace("Sets {} built => {}", key, smashSets.toString().substring(0, 70) + "...");
        } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
            LOGGER.trace("Sets {} built => {}", key, smashSets);
        }
        return smashSets;
    }
}
