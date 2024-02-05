package fr.enimaloc.esportline.api.smash.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.enimaloc.esportline.api.smash.GPSLocation;
import fr.enimaloc.esportline.api.smash.Location;
import fr.enimaloc.esportline.api.smash.SmashBros;
import fr.enimaloc.esportline.api.smash.SmashTournament;
import fr.enimaloc.esportline.api.sql.Column;
import fr.enimaloc.esportline.api.sql.Table;
import fr.enimaloc.esportline.api.sql.WhereStatement;
import fr.enimaloc.esportline.utils.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TournamentTable extends Table {
    private static final int LOG_DETAILS_LEVEL = System.getenv("SMASH_LOG_DETAILS_LEVEL") == null
            ? 0
            : Integer.parseInt(System.getenv("SMASH_LOG_DETAILS_LEVEL").replaceFirst("0x", ""), System.getenv("SMASH_LOG_DETAILS_LEVEL").startsWith("0x") ? 16 : 10);
    public static final Logger LOGGER = LoggerFactory.getLogger(TournamentTable.class);
    public static final ObjectMapper MAPPER = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    public static final Column<String> COLUMN_KEY = new Column<>("key", String.class);
    public static final Column<String> COLUMN_CLEANED_NAME = new Column<>("cleaned_name", String.class);
    public static final Column<String> COLUMN_SOURCE = new Column<>("source", String.class);
    public static final Column<String> COLUMN_TOURNAMENT_NAME = new Column<>("tournament_name", String.class);
    public static final Column<String> COLUMN_TOURNAMENT_EVENT = new Column<>("tournament_event", String.class);
    public static final Column<Integer> COLUMN_SEASON = new Column<>("season", Integer.class);
    public static final Column<String> COLUMN_RANK = new Column<>("rank", String.class);
    public static final Column<Integer> COLUMN_START = new Column<>("start", Integer.class);
    public static final Column<Integer> COLUMN_END = new Column<>("end", Integer.class);
    public static final Column<String> COLUMN_COUNTRY = new Column<>("country", String.class);
    public static final Column<String> COLUMN_STATE = new Column<>("state", String.class);
    public static final Column<String> COLUMN_CITY = new Column<>("city", String.class);
    public static final Column<Integer> COLUMN_ENTRANTS = new Column<>("entrants", Integer.class);
    public static final Column<String> COLUMN_PLACINGS = new Column<>("placings", String.class);
    public static final Column<String> COLUMN_LOSSES = new Column<>("losses", String.class);
    public static final Column<String> COLUMN_BRACKET_TYPES = new Column<>("bracket_types", String.class);
    public static final Column<Boolean> COLUMN_ONLINE = new Column<>("online", Boolean.class);
    public static final Column<Float> COLUMN_LAT = new Column<>("lat", Float.class);
    public static final Column<Float> COLUMN_LNG = new Column<>("lng", Float.class);

    private final SmashBros origin;
    private final SmashBros.SmashGame game;

    public TournamentTable(SmashBros origin, SmashBros.SmashGame game) throws SQLException, IOException {
        this(origin.getConnection(game), origin, game);
    }

    TournamentTable(Connection connection, SmashBros origin, SmashBros.SmashGame game) {
        super(connection, "tournament_info");
        this.origin = origin;
        this.game = game;
    }

    public List<SmashTournament> getTournaments() throws SQLException, JsonProcessingException {
        List<SmashTournament> tournaments = new ArrayList<>();
        try (ResultSet set = select()) {
            while (set.next()) {
                tournaments.add(buildTournament(set));
            }
        }
        return tournaments;
    }

    public SmashTournament getTournament(String key) throws SQLException, JsonProcessingException {
        try (ResultSet set = select(WhereStatement.of(COLUMN_KEY).eq(key))) {
            if (set.next()) {
                return buildTournament(set);
            }
        }
        throw new SQLException("No tournament found with key " + key);
    }

    public List<SmashTournament> getTournamentNames(int limit, int offset) throws SQLException, JsonProcessingException {
        List<SmashTournament> names = new ArrayList<>();
        try (ResultSet set = select(new Column[]{COLUMN_KEY, COLUMN_CLEANED_NAME}, limit, offset)) {
            while (set.next()) {
                names.add(buildTournament(set));
            }
        }
        return names;
    }

    public List<SmashTournament> getTournamentNames(int limit) throws SQLException, JsonProcessingException {
        return getTournamentNames(limit, -1);
    }

    public List<SmashTournament> getTournamentNames() throws SQLException, JsonProcessingException {
        return getTournamentNames(-1, -1);
    }

    public SmashTournament getTournamentByKey(String key) throws SQLException, JsonProcessingException {
        try (ResultSet set = select(WhereStatement.of(COLUMN_KEY).eq(key))) {
            if (set.next()) {
                return buildTournament(set);
            }
        }
        throw new SQLException("Tournament not found");
    }

    private SmashTournament buildTournament(ResultSet set) throws SQLException, JsonProcessingException {
        if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
            LOGGER.trace("Building tournaments {}", set.getString(COLUMN_KEY.getName()));
        }

        boolean tIncomplete = false;
        String tKey = null;
        String tCleanedName = null;
        String tSource = null;
        String tTournamentName = null;
        String tTournamentEvent = null;
        int tSeason = 0;
        char tRank = '\0';
        int tStart = 0;
        int tEnd = 0;
        Location tLocation = null;
        int tEntrants = 0;
        SmashTournament.Placing[] tPlacings = null;
        String tLosses = null;
        String tBracketTypes = null;
        boolean tOnline = false;
        GPSLocation tGpsLocation = null;

        // region Key
        if (SqlUtils.hasColumn(set, COLUMN_KEY.getName())) {
            tKey = set.getString(COLUMN_KEY.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region CleanedName
        if (SqlUtils.hasColumn(set, COLUMN_CLEANED_NAME.getName())) {
            tCleanedName = set.getString(COLUMN_CLEANED_NAME.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Source
        if (SqlUtils.hasColumn(set, COLUMN_SOURCE.getName())) {
            tSource = set.getString(COLUMN_SOURCE.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region TournamentName
        if (SqlUtils.hasColumn(set, COLUMN_TOURNAMENT_NAME.getName())) {
            tTournamentName = set.getString(COLUMN_TOURNAMENT_NAME.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region TournamentEvent
        if (SqlUtils.hasColumn(set, COLUMN_TOURNAMENT_EVENT.getName())) {
            tTournamentEvent = set.getString(COLUMN_TOURNAMENT_EVENT.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Season
        if (SqlUtils.hasColumn(set, COLUMN_SEASON.getName())) {
            tSeason = set.getInt(COLUMN_SEASON.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Rank
        if (SqlUtils.hasColumn(set, COLUMN_RANK.getName())) {
            tRank = set.getString(COLUMN_RANK.getName()).isBlank() ? ' ' : set.getString(COLUMN_RANK.getName()).charAt(0);
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Start
        if (SqlUtils.hasColumn(set, COLUMN_START.getName())) {
            tStart = set.getInt(COLUMN_START.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region End
        if (SqlUtils.hasColumn(set, COLUMN_END.getName())) {
            tEnd = set.getInt(COLUMN_END.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Location
        if (SqlUtils.hasColumn(set, COLUMN_COUNTRY.getName()) && SqlUtils.hasColumn(set, COLUMN_STATE.getName()) && SqlUtils.hasColumn(set, COLUMN_CITY.getName())) {
            String tCountry = set.getString(COLUMN_COUNTRY.getName());
            String tState = set.getString(COLUMN_STATE.getName());
            String tCity = set.getString(COLUMN_CITY.getName());
            tLocation = new Location(tCountry, tState, tCity, null, null, null);
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Entrants
        if (SqlUtils.hasColumn(set, COLUMN_ENTRANTS.getName())) {
            tEntrants = set.getInt(COLUMN_ENTRANTS.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Placings
        if (SqlUtils.hasColumn(set, COLUMN_PLACINGS.getName())) {
            JsonNode placingsNode = MAPPER.readTree(set.getString(COLUMN_PLACINGS.getName()));
            List<SmashTournament.Placing> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashTournament.Placing(origin, game, jsonNode.get(0).asText(), jsonNode.get(1).asInt()));
            }
            tPlacings = placingsList.toArray(SmashTournament.Placing[]::new);
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Losses
        if (SqlUtils.hasColumn(set, COLUMN_LOSSES.getName())) {
            tLosses = set.getString(COLUMN_LOSSES.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region BracketTypes
        if (SqlUtils.hasColumn(set, COLUMN_BRACKET_TYPES.getName())) {
            tBracketTypes = set.getString(COLUMN_BRACKET_TYPES.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region Online
        if (SqlUtils.hasColumn(set, COLUMN_ONLINE.getName())) {
            tOnline = set.getBoolean(COLUMN_ONLINE.getName());
        } else {
            tIncomplete = true;
        }
        // endregion
        // region GPSLocation
        if (SqlUtils.hasColumn(set, COLUMN_LAT.getName()) && SqlUtils.hasColumn(set, COLUMN_LNG.getName())) {
            float tLat = set.getFloat(COLUMN_LAT.getName());
            float tLng = set.getFloat(COLUMN_LNG.getName());
            tGpsLocation = new GPSLocation(tLat, tLng);
        } else {
            tIncomplete = true;
        }
        // endregion
        SmashTournament tournament = new SmashTournament(origin,
                game,
                tIncomplete,
                tKey,
                tCleanedName,
                tSource,
                tTournamentName,
                tTournamentEvent,
                tSeason,
                tRank,
                tStart,
                tEnd,
                tLocation,
                tEntrants,
                tPlacings,
                tLosses,
                tBracketTypes,
                tOnline,
                tGpsLocation);
        if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
            LOGGER.trace("Tournament {} built", tKey);
        } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
            LOGGER.trace("Tournament {} built => {}", tKey, tournament.toString().substring(0, 70) + "...");
        } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
            LOGGER.trace("Tournament {} built => {}", tKey, tournament);
        }
        return tournament;
    }
}
