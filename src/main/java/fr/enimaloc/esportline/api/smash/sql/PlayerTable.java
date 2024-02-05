package fr.enimaloc.esportline.api.smash.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.enimaloc.esportline.api.smash.Location;
import fr.enimaloc.esportline.api.smash.SmashBros;
import fr.enimaloc.esportline.api.smash.SmashPlayer;
import fr.enimaloc.esportline.api.sql.Column;
import fr.enimaloc.esportline.api.sql.Table;
import fr.enimaloc.esportline.api.sql.WhereStatement;
import fr.enimaloc.esportline.utils.SqlUtils;
import org.intellij.lang.annotations.MagicConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerTable extends Table {
    private static final int LOG_DETAILS_LEVEL = System.getenv("SMASH_LOG_DETAILS_LEVEL") == null
            ? 0
            : Integer.parseInt(System.getenv("SMASH_LOG_DETAILS_LEVEL").replaceFirst("0x", ""), System.getenv("SMASH_LOG_DETAILS_LEVEL").startsWith("0x") ? 16 : 10);
    public static final Logger LOGGER = LoggerFactory.getLogger(TournamentTable.class);
    public static final ObjectMapper MAPPER = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    public static final Column<String> COLUMN_PLAYER_ID = new Column<>("player_id", String.class);
    public static final Column<String> COLUMN_TAG = new Column<>("tag", String.class);
    public static final Column<String> COLUMN_ALL_TAGS = new Column<>("all_tags", String.class);
    public static final Column<String> COLUMN_PREFIXES = new Column<>("prefixes", String.class);
    public static final Column<String> COLUMN_SOCIAL = new Column<>("social", String.class);
    public static final Column<String> COLUMN_COUNTRY = new Column<>("country", String.class);
    public static final Column<String> COLUMN_STATE = new Column<>("state", String.class);
    public static final Column<String> COLUMN_REGION = new Column<>("region", String.class);
    public static final Column<String> COLUMN_C_COUNTRY = new Column<>("c_country", String.class);
    public static final Column<String> COLUMN_C_STATE = new Column<>("c_state", String.class);
    public static final Column<String> COLUMN_C_REGION = new Column<>("c_region", String.class);
    public static final Column<String> COLUMN_PLACINGS = new Column<>("placings", String.class);
    public static final Column<String> COLUMN_CHARACTERS = new Column<>("characters", String.class);
    public static final Column<String> COLUMN_ALIAS = new Column<>("alias", String.class);

    private final SmashBros origin;
    private final SmashBros.SmashGame game;

    public PlayerTable(SmashBros origin, SmashBros.SmashGame game) throws SQLException, IOException {
        this(origin.getConnection(game), origin, game);
    }

    PlayerTable(Connection sql, SmashBros origin, SmashBros.SmashGame game) {
        super(sql, "players");
        this.origin = origin;
        this.game = game;
    }

    public List<SmashPlayer> getPlayers() throws SQLException, JsonProcessingException {
        return filterPlayers(null);
    }

    public SmashPlayer getPlayerById(String playerId) throws SQLException, JsonProcessingException {
        return filterPlayers(getIdFilter(playerId)).get(0);
    }

    public List<SmashPlayer> getPlayersByTag(String tag) throws SQLException, JsonProcessingException {
        return filterPlayers(getTagFilter(tag));
    }

    public List<SmashPlayer> getPlayersByIdOrTag(String idOrTag) throws SQLException, JsonProcessingException {
        return filterPlayers(getTagOrIdFilter(idOrTag));
    }

    public List<SmashPlayer> getPlayersByPrefix(String prefix) throws SQLException, JsonProcessingException {
        return filterPlayers(getPrefixFilter(prefix));
    }

    public List<SmashPlayer> getPlayersByExactLocation(Location location) throws SQLException, JsonProcessingException {
        return filterPlayers(WhereStatement.of(COLUMN_COUNTRY).eq(location.country())
                .and(WhereStatement.of(COLUMN_STATE).eq(location.state()))
                .and(WhereStatement.of(COLUMN_REGION).eq(location.city()))
                .and(WhereStatement.of(COLUMN_C_COUNTRY).eq(location.cCountry()))
                .and(WhereStatement.of(COLUMN_C_STATE).eq(location.cState()))
                .and(WhereStatement.of(COLUMN_C_REGION).eq(location.cCity())));
    }

    public List<SmashPlayer> getPlayersByCountry(String country) throws SQLException, JsonProcessingException {
        return filterPlayers(getCountryFilter(country));
    }

    public List<SmashPlayer> getPlayersByState(String state) throws SQLException, JsonProcessingException {
        return filterPlayers(getStateFilter(state));
    }

    public List<SmashPlayer> getPlayersByCity(String city) throws SQLException, JsonProcessingException {
        return filterPlayers(getCityFilter(city));
    }

    public List<SmashPlayer> filterPlayers(WhereStatement where) throws SQLException, JsonProcessingException {
        List<SmashPlayer> players = new ArrayList<>();
        try (ResultSet set = select(where)) {
            while (set.next()) {
                players.add(buildPlayer(set));
            }
        }
        return players;
    }

    public List<SmashPlayer> getPlayersName() throws SQLException, JsonProcessingException {
        List<SmashPlayer> players = new ArrayList<>();
        try (ResultSet set = select(new Column[]{COLUMN_PLAYER_ID, COLUMN_TAG})) {
            while (set.next()) {
                players.add(buildPlayer(set));
            }
        }
        return players;
    }

    public List<SmashPlayer> getPlayersName(WhereStatement where) throws SQLException, JsonProcessingException {
        List<SmashPlayer> players = new ArrayList<>();
        try (ResultSet set = select(new Column[]{COLUMN_PLAYER_ID, COLUMN_TAG}, where)) {
            while (set.next()) {
                players.add(buildPlayer(set));
            }
        }
        return players;
    }

    public List<SmashPlayer> getPlayersName(WhereStatement where, int limit, int offset) throws SQLException, JsonProcessingException {
        List<SmashPlayer> players = new ArrayList<>();
        try (ResultSet set = select(new Column[]{COLUMN_PLAYER_ID, COLUMN_TAG}, where, limit, offset)) {
            while (set.next()) {
                players.add(buildPlayer(set));
            }
        }
        return players;
    }

    public List<SmashPlayer> getPlayersName(int limit, int offset) throws SQLException, JsonProcessingException {
        return getPlayersName(null, limit, offset);
    }

    public List<SmashPlayer> getPlayersName(int limit) throws SQLException, JsonProcessingException {
        return getPlayersName(limit, 0);
    }

    private SmashPlayer buildPlayer(ResultSet set) throws SQLException, JsonProcessingException {
        if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
            LOGGER.trace("Building player {}", set.getInt(COLUMN_PLAYER_ID.getName()));
        }

        boolean complete = true;

        int playerId = -1;
        String pTag = null;
        String[] allTags = null;
        String[] prefixes = null;
        SmashPlayer.Social[] socials = null;
        Location location = null;
        SmashPlayer.Placings[] placings = null;
        SmashPlayer.Character[] characters = null;
        String alias = null;

        // region Player ID
        if (SqlUtils.hasColumn(set, COLUMN_PLAYER_ID)) {
            playerId = set.getInt(COLUMN_PLAYER_ID.getName());
        } else {
            complete = false;
        }
        // endregion
        // region Tag
        if (SqlUtils.hasColumn(set, COLUMN_TAG)) {
            pTag = set.getString(COLUMN_TAG.getName());
        } else {
            complete = false;
        }
        // endregion
        // region All tags
        if (SqlUtils.hasColumn(set, COLUMN_ALL_TAGS)) {
            JsonNode allTagsNode = MAPPER.readTree(set.getString(COLUMN_ALL_TAGS.getName()));
            allTags = new String[allTagsNode.size()];
            for (int i = 0; i < allTagsNode.size(); i++) {
                allTags[i] = allTagsNode.get(i).asText();
            }
        } else {
            complete = false;
        }
        // endregion
        // region Prefixes
        if (SqlUtils.hasColumn(set, COLUMN_PREFIXES)) {
            JsonNode prefixesNode = MAPPER.readTree(set.getString(COLUMN_PREFIXES.getName()));
            prefixes = new String[prefixesNode.size()];
            for (int i = 0; i < prefixesNode.size(); i++) {
                prefixes[i] = prefixesNode.get(i).asText();
            }
        } else {
            complete = false;
        }
        // endregion
        // region Socials
        if (SqlUtils.hasColumn(set, COLUMN_SOCIAL)) {
            JsonNode socialNode = MAPPER.readTree(set.getString(COLUMN_SOCIAL.getName()));
            List<SmashPlayer.Social> socialsList = new ArrayList<>();
            for (Iterator<String> it = socialNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                List<String> values = new ArrayList<>();
                for (JsonNode jsonNode : socialNode.get(key)) {
                    values.add(jsonNode.asText());
                }
                socialsList.add(new SmashPlayer.Social(key, values.toArray(String[]::new)));
            }
            socials = socialsList.toArray(SmashPlayer.Social[]::new);
        } else {
            complete = false;
        }// endregion
        // region Location
        if (SqlUtils.hasColumn(set, COLUMN_COUNTRY) && SqlUtils.hasColumn(set, COLUMN_STATE) && SqlUtils.hasColumn(set, COLUMN_REGION)
                && SqlUtils.hasColumn(set, COLUMN_C_COUNTRY) && SqlUtils.hasColumn(set, COLUMN_C_STATE) && SqlUtils.hasColumn(set, COLUMN_C_REGION)) {
            String country = set.getString(COLUMN_COUNTRY.getName());
            String state = set.getString(COLUMN_STATE.getName());
            String region = set.getString(COLUMN_REGION.getName());
            String cCountry = set.getString(COLUMN_C_COUNTRY.getName());
            String cState = set.getString(COLUMN_C_STATE.getName());
            String cRegion = set.getString(COLUMN_C_REGION.getName());
            location = new Location(country, state, region, cCountry, cState, cRegion);
        } else {
            complete = false;
        }
        // endregion
        // region Placings
        if (SqlUtils.hasColumn(set, COLUMN_PLACINGS)) {
            JsonNode placingsNode = MAPPER.readTree(set.getString(COLUMN_PLACINGS.getName()));
            List<SmashPlayer.Placings> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashPlayer.Placings(origin, game, playerId, jsonNode));
            }
            placings = placingsList.toArray(SmashPlayer.Placings[]::new);
        } else {
            complete = false;
        }
        // endregion
        // region Characters
        if (SqlUtils.hasColumn(set, COLUMN_CHARACTERS)) {
            JsonNode charactersNode = MAPPER.readTree(set.getString(COLUMN_CHARACTERS.getName()));
            List<SmashPlayer.Character> charactersList = new ArrayList<>();
            for (Iterator<String> it = charactersNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                charactersList.add(new SmashPlayer.Character(key, charactersNode.get(key).asInt()));
            }
            characters = charactersList.toArray(SmashPlayer.Character[]::new);
        } else {
            complete = false;
        }
        // endregion
        // region Alias
        if (SqlUtils.hasColumn(set, COLUMN_ALIAS)) {
            alias = set.getString(COLUMN_ALIAS.getName());
        } else {
            complete = false;
        }
        // endregion
        SmashPlayer player = new SmashPlayer(
                origin,
                game,
                !complete,
                playerId,
                pTag,
                allTags,
                prefixes,
                socials,
                location,
                placings,
                characters,
                alias
        );
        if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
            LOGGER.trace("Player {} built", playerId);
        } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
            LOGGER.trace("Player {} built => {}", playerId, player.toString().substring(0, 70) + "...");
        } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
            LOGGER.trace("Player {} built => {}", playerId, player);
        }
        return player;
    }

    public static WhereStatement<?> getCityFilter(String city) {
        return
                WhereStatement.of(COLUMN_REGION).eq(city)
                        .or(WhereStatement.of(COLUMN_C_REGION).eq(city));
    }

    public static WhereStatement<?> getStateFilter(String state) {
        return WhereStatement.of(COLUMN_STATE).eq(state)
                .or(WhereStatement.of(COLUMN_C_STATE).eq(state));
    }

    public static WhereStatement<?> getCountryFilter(String country) {
        return WhereStatement.of(COLUMN_COUNTRY).eq(country)
                .or(WhereStatement.of(COLUMN_C_COUNTRY).eq(country));
    }

    public static WhereStatement<String> getPrefixFilter(String prefix) {
        return getPrefixFilter(prefix, WhereStatement.WILDCARD_START_ZERO_OR_MORE, WhereStatement.WILDCARD_END_ZERO_OR_MORE);
    }

    public static WhereStatement<String> getPrefixFilter(String prefix, @MagicConstant(flags = {
            WhereStatement.WILDCARD_START_ONE,
            WhereStatement.WILDCARD_START_ZERO_OR_MORE,
            WhereStatement.WILDCARD_END_ONE,
            WhereStatement.WILDCARD_END_ZERO_OR_MORE
    }) int... wildcard) {
        return WhereStatement.of(COLUMN_PREFIXES).like("\"" + prefix + "\"", wildcard);
    }

    public static WhereStatement<String> getPrefixFilter(String prefix, @MagicConstant(flags = {
            WhereStatement.WILDCARD_START_ONE,
            WhereStatement.WILDCARD_START_ZERO_OR_MORE,
            WhereStatement.WILDCARD_END_ONE,
            WhereStatement.WILDCARD_END_ZERO_OR_MORE
    }) int wildcard) {
        return WhereStatement.of(COLUMN_PREFIXES).like("\"" + prefix + "\"", wildcard);
    }

    public static WhereStatement<String> getTagFilter(String tag) {
        return WhereStatement.of(COLUMN_TAG).eq(tag);
    }

    public static WhereStatement<String> getIdFilter(String id) {
        return WhereStatement.of(COLUMN_PLAYER_ID).eq(id);
    }

    public static WhereStatement<String> getTagOrIdFilter(String idOrTag) {
        return WhereStatement.of(COLUMN_PLAYER_ID).eq(idOrTag)
                .or(WhereStatement.of(COLUMN_TAG).eq(idOrTag));
    }

    public static WhereStatement<String> getTagOrIdFilter(String id, String tag) {
        return WhereStatement.of(COLUMN_PLAYER_ID).eq(id)
                .or(WhereStatement.of(COLUMN_TAG).eq(tag));
    }
}
