package fr.enimaloc.esportline.commands.slash.game.smash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.enimaloc.esportline.api.github.Github;
import fr.enimaloc.esportline.api.github.LatestGithubRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SmashBros {
    public static final Logger LOGGER = LoggerFactory.getLogger(SmashBros.class);
    public static final String GITHUB_ORG = "smashdata";
    public static final String GITHUB_REPO = "ThePlayerDatabase";
    private static final int LOG_DETAILS_LEVEL = System.getenv("SMASH_LOG_DETAILS_LEVEL") == null
            ? 0
            : Integer.parseInt(System.getenv("SMASH_LOG_DETAILS_LEVEL").replaceFirst("0x", ""), System.getenv("SMASH_LOG_DETAILS_LEVEL").startsWith("0x") ? 16 : 10);
    private File smashData = new File("data/smash");
    private Map<SmashGame, List<SmashPlayer>> cachePlayers = new HashMap<>();
    private Map<SmashGame, List<SmashTournament>> cacheTournaments = new HashMap<>();
    private Map<SmashGame, List<SmashSets>> cacheSets = new HashMap<>();

    private File getData(SmashGame game) throws IOException {
        if (!smashData.exists()) {
            smashData.mkdirs();
        }
        LatestGithubRelease release = Github.getLastRelease(GITHUB_ORG, GITHUB_REPO);
        File dbFile = new File(smashData, release.tagName() + "_" + game.getFileName().replace(".zip", ".db"));
        LOGGER.debug("Checking if {} exists", dbFile);
        if (dbFile.exists()) {
            LOGGER.debug("{} exists, passing download and extracting", dbFile);
            return dbFile;
        }
        LOGGER.debug("{} doesn't exist, downloading...", dbFile);
        // region Download ZIP
        File zip = new File(smashData, release.tagName() + "_" + game.getFileName());
        LOGGER.debug("Checking if {} exists", zip);
        if (!zip.exists()) {
            LOGGER.debug("{} doesn't exist, downloading...", zip);
            URL url = URI.create(Arrays.stream(release.assets())
                            .filter(asset -> asset.name().equals(game.getFileName())).findFirst().orElseThrow().browserDownloadUrl())
                    .toURL();
            try (InputStream in = url.openStream()) {
                LOGGER.trace("Copying {} to {}", url, zip);
                Files.copy(in, zip.toPath());
                LOGGER.trace("{} copied", zip);
            }
            LOGGER.debug("{} downloaded", zip);
        }
        // endregion
        // region Unzip
        LOGGER.debug("Extracting {} to {}", zip, dbFile);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                LOGGER.trace("Checking eligibility of {}", entry.getName());
                if (entry.getName().endsWith(".db")) {
                    LOGGER.trace("{} is eligible, copying...", entry.getName());
                    Files.copy(zis, dbFile.toPath());
                    LOGGER.trace("{} copied", entry.getName());
                }
                entry = zis.getNextEntry();
            }
        }
        LOGGER.debug("{} extracted", zip);
        // endregion
        return dbFile;
    }

    private Connection getConnection(SmashGame game) throws IOException, SQLException {
        File dbFile = getData(game);
        LOGGER.debug("Getting connection to {}", dbFile);
        Connection sql = DriverManager.getConnection(JDBC.PREFIX + dbFile.getAbsolutePath());
        LOGGER.debug("Connection to {} got", dbFile);
        return sql;
    }

    public List<SmashPlayer> getPlayers(SmashGame game) throws SQLException, IOException {
        LOGGER.debug("Getting players of {}", game);
        if (cachePlayers.containsKey(game)) {
            LOGGER.debug("Players of {} already cached, returning", game);
            return cachePlayers.get(game);
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM players\" on {}", connection);
        List<SmashPlayer> players = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM players");
        while (set.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building player {}", set.getInt("player_id"));
            }
            int playerId = set.getInt("player_id");
            String tag = set.getString("tag");
            // region All tags
            JsonNode allTagsNode = mapper.readTree(set.getString("all_tags"));
            String[] allTags = new String[allTagsNode.size()];
            for (int i = 0; i < allTagsNode.size(); i++) {
                allTags[i] = allTagsNode.get(i).asText();
            }
            // endregion
            // region Prefixes
            JsonNode prefixesNode = mapper.readTree(set.getString("prefixes"));
            String[] prefixes = new String[prefixesNode.size()];
            for (int i = 0; i < prefixesNode.size(); i++) {
                prefixes[i] = prefixesNode.get(i).asText();
            }
            // endregion
            // region Socials
            JsonNode socialNode = mapper.readTree(set.getString("social"));
            List<SmashPlayer.Social> socialsList = new ArrayList<>();
            for (Iterator<String> it = socialNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                List<String> values = new ArrayList<>();
                for (JsonNode jsonNode : socialNode.get(key)) {
                    values.add(jsonNode.asText());
                }
                socialsList.add(new SmashPlayer.Social(key, values.toArray(String[]::new)));
            }
            SmashPlayer.Social[] socials = socialsList.toArray(SmashPlayer.Social[]::new);
            // endregion
            // region Location
            String country = set.getString("country");
            String state = set.getString("state");
            String region = set.getString("region");
            String cCountry = set.getString("c_country");
            String cState = set.getString("c_state");
            String cRegion = set.getString("c_region");
            Location location = new Location(country, state, region, cCountry, cState, cRegion);
            // endregion
            // region Placings
            JsonNode placingsNode = mapper.readTree(set.getString("placings"));
            List<SmashPlayer.Placings> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashPlayer.Placings(this, game, playerId, jsonNode));
            }
            SmashPlayer.Placings[] placings = placingsList.toArray(SmashPlayer.Placings[]::new);
            // endregion
            // region Characters
            JsonNode charactersNode = mapper.readTree(set.getString("characters"));
            List<SmashPlayer.Character> charactersList = new ArrayList<>();
            for (Iterator<String> it = charactersNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                charactersList.add(new SmashPlayer.Character(key, charactersNode.get(key).asInt()));
            }
            SmashPlayer.Character[] characters = charactersList.toArray(SmashPlayer.Character[]::new);
            // endregion
            String alias = set.getString("alias");
            SmashPlayer player = new SmashPlayer(this, game, playerId, tag, allTags, prefixes, socials, location, placings, characters, alias);
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Player {} built", playerId);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Player {} built => {}", playerId, player.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Player {} built => {}", playerId, player);
            }
            players.add(player);
        }
        long end = System.currentTimeMillis();
        cachePlayers.put(game, players);
        LOGGER.debug("Got {} players of {} in {}ms", players.size(), game, end - start);
        return players;
    }

    public SmashPlayer getPlayer(SmashGame game, String playerIdentifier) throws SQLException, IOException {
        return getPlayer(game, playerIdentifier.matches("\\d+") ? Integer.parseInt(playerIdentifier) : -1, playerIdentifier.matches("\\d+") ? "" : playerIdentifier);
    }

    public SmashPlayer getPlayer(SmashGame game, int id, String tag) throws SQLException, IOException {
        LOGGER.debug("Getting player {} of {}", id, game);
        if (cachePlayers.containsKey(game)) {
            LOGGER.debug("Players of {} already cached, searching {} among us", game, id);
            return cachePlayers.get(game).stream().filter(smashPlayer -> smashPlayer.playerId() == id || smashPlayer.tag().equals(tag)).findFirst().orElseGet(() -> {
                LOGGER.debug("Player {} not found among us, invalidating players and recall", id);
                invalidatePlayer(game);
                try {
                    return getPlayer(game, id, tag);
                } catch (SQLException | IOException e) {
                    LOGGER.error("Error while getting player {} of {}", id, game, e);
                    return null;
                }
            });
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM players WHERE player_id = {} OR tag = '{}'\" on {}", id, tag, connection);
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        SmashPlayer player = null;
        long start = System.currentTimeMillis();
        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM players WHERE player_id = " + id + " OR tag = '" + tag + "'");
        while (set.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building player {}", set.getInt("player_id"));
            }
            int playerId = set.getInt("player_id");
            String pTag = set.getString("tag");
            // region All tags
            JsonNode allTagsNode = mapper.readTree(set.getString("all_tags"));
            String[] allTags = new String[allTagsNode.size()];
            for (int i = 0; i < allTagsNode.size(); i++) {
                allTags[i] = allTagsNode.get(i).asText();
            }
            // endregion
            // region Prefixes
            JsonNode prefixesNode = mapper.readTree(set.getString("prefixes"));
            String[] prefixes = new String[prefixesNode.size()];
            for (int i = 0; i < prefixesNode.size(); i++) {
                prefixes[i] = prefixesNode.get(i).asText();
            }
            // endregion
            // region Socials
            JsonNode socialNode = mapper.readTree(set.getString("social"));
            List<SmashPlayer.Social> socialsList = new ArrayList<>();
            for (Iterator<String> it = socialNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                List<String> values = new ArrayList<>();
                for (JsonNode jsonNode : socialNode.get(key)) {
                    values.add(jsonNode.asText());
                }
                socialsList.add(new SmashPlayer.Social(key, values.toArray(String[]::new)));
            }
            SmashPlayer.Social[] socials = socialsList.toArray(SmashPlayer.Social[]::new);
            // endregion
            // region Location
            String country = set.getString("country");
            String state = set.getString("state");
            String region = set.getString("region");
            String cCountry = set.getString("c_country");
            String cState = set.getString("c_state");
            String cRegion = set.getString("c_region");
            Location location = new Location(country, state, region, cCountry, cState, cRegion);
            // endregion
            // region Placings
            JsonNode placingsNode = mapper.readTree(set.getString("placings"));
            List<SmashPlayer.Placings> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashPlayer.Placings(this, game, playerId, jsonNode));
            }
            SmashPlayer.Placings[] placings = placingsList.toArray(SmashPlayer.Placings[]::new);
            // endregion
            // region Characters
            JsonNode charactersNode = mapper.readTree(set.getString("characters"));
            List<SmashPlayer.Character> charactersList = new ArrayList<>();
            for (Iterator<String> it = charactersNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                charactersList.add(new SmashPlayer.Character(key, charactersNode.get(key).asInt()));
            }
            SmashPlayer.Character[] characters = charactersList.toArray(SmashPlayer.Character[]::new);
            // endregion
            String alias = set.getString("alias");
            player = new SmashPlayer(this, game, playerId, pTag, allTags, prefixes, socials, location, placings, characters, alias);
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Player {} built", playerId);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Player {} built => {}", playerId, player.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Player {} built => {}", playerId, player);
            }
        }
        long end = System.currentTimeMillis();
        if (player == null) {
            LOGGER.debug("Player {} not found in {}", id, game);
            throw new SQLException("Player " + id + " not found in " + game);
        }
        LOGGER.debug("Got player {} of {} in {}ms", player.playerId(), game, end - start);
        return player;
    }

    public List<SmashTournament> getTournaments(SmashGame game) throws SQLException, IOException {
        LOGGER.debug("Getting tournaments of {}", game);
        if (cacheTournaments.containsKey(game)) {
            LOGGER.debug("Tournaments of {} already cached, returning", game);
            return cacheTournaments.get(game);
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM tournament_info\" on {}", connection);
        List<SmashTournament> tournaments = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM tournament_info");
        while (set.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building tournaments {}", set.getString("key"));
            }
            String key = set.getString("key");
            String cleanedName = set.getString("cleaned_name");
            String source = set.getString("source");
            String tournamentName = set.getString("tournament_name");
            String tournamentEvent = set.getString("tournament_event");
            int season = set.getInt("season");
            char rank = set.getString("rank").isBlank() ? ' ' : set.getString("rank").charAt(0);
            long startT = set.getLong("start");
            long endT = set.getLong("end");
            // region Location
            String country = set.getString("country");
            String state = set.getString("state");
            String city = set.getString("city");
            Location location = new Location(country, state, city, null, null, null);
            // endregion
            int entrants = set.getInt("entrants");
            // region Placings
            JsonNode placingsNode = mapper.readTree(set.getString("placings"));
            List<SmashTournament.Placing> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashTournament.Placing(this, game, jsonNode.get(0).asText(), jsonNode.get(1).asInt()));
            }
            SmashTournament.Placing[] placings = placingsList.toArray(SmashTournament.Placing[]::new);
            String losses = set.getString("losses");
            String bracketTypes = set.getString("bracket_types");
            boolean online = set.getBoolean("online");
            // region GPSLocation
            float lat = set.getFloat("lat");
            float lng = set.getFloat("lng");
            GPSLocation gpsLocation = new GPSLocation(lat, lng);
            // endregion
            SmashTournament tournament = new SmashTournament(this, game, key, cleanedName, source, tournamentName, tournamentEvent, season, rank, startT, endT, location, entrants, placings, losses, bracketTypes, online, gpsLocation);
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Tournaments {} built", key);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Tournaments {} built => {}", key, tournament.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Tournaments {} built => {}", key, tournament);
            }
            tournaments.add(tournament);
        }
        long end = System.currentTimeMillis();
        cacheTournaments.put(game, tournaments);
        LOGGER.debug("Got {} tournaments of {} in {}ms", tournaments.size(), game, end - start);
        return tournaments;
    }

    public SmashTournament getTournament(SmashGame game, String key) throws SQLException, IOException {
        LOGGER.debug("Getting tournament {} of {}", key, game);
        if (cacheTournaments.containsKey(game)) {
            LOGGER.debug("Tournaments of {} already cached, searching {} among us", game, key);
            return cacheTournaments.get(game).stream().filter(smashTournament -> smashTournament.key().equals(key)).findFirst().orElseGet(() -> {
                LOGGER.debug("Tournament {} not found among us, invalidating tournaments and recall", key);
                invalidateTournament(game);
                try {
                    return getTournament(game, key);
                } catch (SQLException | IOException e) {
                    LOGGER.error("Error while getting tournament {} of {}", key, game, e);
                    return null;
                }
            });
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM tournament_info WHERE key = '{}'\" on {}", key, connection);
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        SmashTournament tournament = null;
        long start = System.currentTimeMillis();
        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM tournament_info WHERE key = '" + key + "'");
        while (set.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building tournaments {}", set.getString("key"));
            }
            String tKey = set.getString("key");
            String cleanedName = set.getString("cleaned_name");
            String source = set.getString("source");
            String tournamentName = set.getString("tournament_name");
            String tournamentEvent = set.getString("tournament_event");
            int season = set.getInt("season");
            char rank = set.getString("rank").isBlank() ? ' ' : set.getString("rank").charAt(0);
            long startT = set.getLong("start");
            long endT = set.getLong("end");
            // region Location
            String country = set.getString("country");
            String state = set.getString("state");
            String city = set.getString("city");
            Location location = new Location(country, state, city, null, null, null);
            // endregion
            int entrants = set.getInt("entrants");
            // region Placings
            JsonNode placingsNode = mapper.readTree(set.getString("placings"));
            List<SmashTournament.Placing> placingsList = new ArrayList<>();
            for (JsonNode jsonNode : placingsNode) {
                placingsList.add(new SmashTournament.Placing(this, game, jsonNode.get(0).asText(), jsonNode.get(1).asInt()));
            }
            SmashTournament.Placing[] placings = placingsList.toArray(SmashTournament.Placing[]::new);
            String losses = set.getString("losses");
            String bracketTypes = set.getString("bracket_types");
            boolean online = set.getBoolean("online");
            // region GPSLocation
            float lat = set.getFloat("lat");
            float lng = set.getFloat("lng");
            GPSLocation gpsLocation = new GPSLocation(lat, lng);
            // endregion
            tournament = new SmashTournament(this, game, tKey, cleanedName, source, tournamentName, tournamentEvent, season, rank, startT, endT, location, entrants, placings, losses, bracketTypes, online, gpsLocation);
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Tournaments {} built", tKey);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Tournaments {} built => {}", tKey, tournament.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Tournaments {} built => {}", tKey, tournament);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Got tournament {} of {} in {}ms", tournament.key(), game, end - start);
        return tournament;
    }

    public List<SmashSets> getSets(SmashGame game) throws SQLException, IOException {
        LOGGER.debug("Getting sets of {}", game);
        if (cacheSets.containsKey(game)) {
            LOGGER.debug("Sets of {} already cached, returning", game);
            return cacheSets.get(game);
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM sets\" on {}", connection);
        List<SmashSets> sets = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        ResultSet set = connection.createStatement().executeQuery("SELECT * FROM sets");
        while (set.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building sets {}", set.getString("key"));
            }
            String key = set.getString("key");
            String tournamentKey = set.getString("tournament_key");
            String winnerId = set.getString("winner_id");
            String player1Id = set.getString("p1_id");
            String player2Id = set.getString("p2_id");
            int player1Score = set.getInt("p1_score");
            int player2Score = set.getInt("p2_score");
            // region LocationNames
            JsonNode locationNamesNode = mapper.readTree(set.getString("location_names"));
            String[] locationNames = new String[locationNamesNode.size()];
            for (int i = 0; i < locationNamesNode.size(); i++) {
                locationNames[i] = locationNamesNode.get(i).asText();
            }
            // endregion
            String bracketName = set.getString("bracket_name");
            String bracketOrder = set.getString("bracket_order");
            String setOrder = set.getString("set_order");
            int bestOf = set.getInt("best_of");
            String gameData = set.getString("game_data");
            SmashSets smashSets = new SmashSets(this,
                    game,
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
                    mapper.readValue(gameData, SmashSets.GameData[].class));
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Sets {} built", key);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Sets {} built => {}", key, smashSets.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Sets {} built => {}", key, smashSets);
            }
            sets.add(smashSets);
        }
        long end = System.currentTimeMillis();
        cacheSets.put(game, sets);
        LOGGER.debug("Got {} sets of {} in {}ms", sets.size(), game, end - start);
        return sets;
    }

    public List<SmashSets> getSetsForTournament(SmashGame game, String tournamentKey) throws SQLException, IOException {
        LOGGER.debug("Getting sets of {} for tournament {}", game, tournamentKey);
        if (cacheSets.containsKey(game)) {
            LOGGER.debug("Sets of {} already cached, searching {} among us", game, tournamentKey);
            List<SmashSets> sets = cacheSets.get(game).stream().filter(smashSets -> smashSets.tournamentKey().equals(tournamentKey)).toList();
            if (sets.isEmpty()) {
                LOGGER.debug("Sets of {} for tournament {} not found among us, invalidating sets and recall", game, tournamentKey);
                invalidateSets(game);
                return getSetsForTournament(game, tournamentKey);
            }
            return sets;
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM sets WHERE tournament_key = '{}'\" on {}", tournamentKey, connection);
        List<SmashSets> sets = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        try (ResultSet set = connection.createStatement().executeQuery("SELECT * FROM sets WHERE tournament_key = '" + tournamentKey + "'")) {
            while (set.next()) {
                if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                    LOGGER.trace("Building sets {}", set.getString("key"));
                }
                String key = set.getString("key");
                String winnerId = set.getString("winner_id");
                String player1Id = set.getString("p1_id");
                String player2Id = set.getString("p2_id");
                int player1Score = set.getInt("p1_score");
                int player2Score = set.getInt("p2_score");
                // region LocationNames
                JsonNode locationNamesNode = mapper.readTree(set.getString("location_names"));
                String[] locationNames = new String[locationNamesNode.size()];
                for (int i = 0; i < locationNamesNode.size(); i++) {
                    locationNames[i] = locationNamesNode.get(i).asText();
                }
                // endregion
                String bracketName = set.getString("bracket_name");
                String bracketOrder = set.getString("bracket_order");
                String setOrder = set.getString("set_order");
                int bestOf = set.getInt("best_of");
                String gameData = set.getString("game_data");
                SmashSets smashSets = new SmashSets(this,
                        game,
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
                        mapper.readValue(gameData, SmashSets.GameData[].class));
                if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                    LOGGER.trace("Sets {} built", key);
                } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                    LOGGER.trace("Sets {} built => {}", key, smashSets.toString().substring(0, 70) + "...");
                } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                    LOGGER.trace("Sets {} built => {}", key, smashSets);
                }
                sets.add(smashSets);
            }
        } catch (SQLException e) {
            LOGGER.error("Error while getting sets of {} for tournament {}", game, tournamentKey, e);
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Got {} sets of {} for tournament {} in {}ms", sets.size(), game, tournamentKey, end - start);
        return sets;
    }

    public List<SmashSets> getSetsForTournamentAndPlayer(SmashGame game, String tournamentKey, String playerId) throws SQLException, IOException {
        LOGGER.debug("Getting sets of {} for tournament {} and player {}", game, tournamentKey, playerId);
        if (cacheSets.containsKey(game)) {
            LOGGER.debug("Sets of {} already cached, searching {} among us", game, tournamentKey);
            List<SmashSets> sets = cacheSets.get(game).stream().filter(smashSets -> smashSets.tournamentKey().equals(tournamentKey) && (smashSets.player1Id().equals(playerId) || smashSets.player2Id().equals(playerId))).toList();
            if (sets.isEmpty()) {
                LOGGER.debug("Sets of {} for tournament {} and player {} not found among us, invalidating sets and recall", game, tournamentKey, playerId);
                invalidateSets(game);
                return getSetsForTournamentAndPlayer(game, tournamentKey, playerId);
            }
            return sets;
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM sets WHERE tournament_key = '{}' AND (p1_id = '{}' OR p2_id = '{}')\" on {}", tournamentKey, playerId, playerId, connection);
        List<SmashSets> sets = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        try (ResultSet set = connection.createStatement().executeQuery("SELECT * FROM sets WHERE tournament_key = '" + tournamentKey + "' AND (p1_id = '" + playerId + "' OR p2_id = '" + playerId + "')")) {
            while (set.next()) {
                if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                    LOGGER.trace("Building sets {}", set.getString("key"));
                }
                String key = set.getString("key");
                String winnerId = set.getString("winner_id");
                String player1Id = set.getString("p1_id");
                String player2Id = set.getString("p2_id");
                int player1Score = set.getInt("p1_score");
                int player2Score = set.getInt("p2_score");
                // region LocationNames
                JsonNode locationNamesNode = mapper.readTree(set.getString("location_names"));
                String[] locationNames = new String[locationNamesNode.size()];
                for (int i = 0; i < locationNamesNode.size(); i++) {
                    locationNames[i] = locationNamesNode.get(i).asText();
                }
                // endregion
                String bracketName = set.getString("bracket_name");
                String bracketOrder = set.getString("bracket_order");
                String setOrder = set.getString("set_order");
                int bestOf = set.getInt("best_of");
                String gameData = set.getString("game_data");
                SmashSets smashSets = new SmashSets(this,
                        game,
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
                        mapper.readValue(gameData, SmashSets.GameData[].class));
                if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                    LOGGER.trace("Sets {} built", key);
                } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                    LOGGER.trace("Sets {} built => {}", key, smashSets.toString().substring(0, 70) + "...");
                } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                    LOGGER.trace("Sets {} built => {}", key, smashSets);
                }
                sets.add(smashSets);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Got {} sets of {} for tournament {} and player {} in {}ms", sets.size(), game, tournamentKey, playerId, end - start);
        return sets;
    }

    public List<SmashSets> getSetsForPlayer(SmashGame game, String playerId) throws SQLException, IOException {
        LOGGER.debug("Getting sets of {} for player {}", game, playerId);
        if (cacheSets.containsKey(game)) {
            LOGGER.debug("Sets of {} already cached, searching {} among us", game, playerId);
            List<SmashSets> sets = cacheSets.get(game).stream().filter(smashSets -> smashSets.player1Id().equals(playerId) || smashSets.player2Id().equals(playerId)).toList();
            if (sets.isEmpty()) {
                LOGGER.debug("Sets of {} for player {} not found among us, invalidating sets and recall", game, playerId);
                invalidateSets(game);
                return getSetsForPlayer(game, playerId);
            }
            return sets;
        }
        Connection connection = getConnection(game);
        LOGGER.trace("Executing query \"SELECT * FROM sets WHERE p1_id = '{}' OR p2_id = '{}'\" on {}", playerId, playerId, connection);
        List<SmashSets> sets = new ArrayList<>();
        ObjectMapper mapper = new JsonMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        long start = System.currentTimeMillis();
        ResultSet result = connection.createStatement().executeQuery("SELECT * FROM sets WHERE p1_id = '" + playerId + "' OR p2_id = '" + playerId + "'");
        while (result.next()) {
            if ((LOG_DETAILS_LEVEL & 0x10) == 0x10) {
                LOGGER.trace("Building sets {}", result.getString("key"));
            }
            String key = result.getString("key");
            String tournamentKey = result.getString("tournament_key");
            String winnerId = result.getString("winner_id");
            String player1Id = result.getString("p1_id");
            String player2Id = result.getString("p2_id");
            int player1Score = result.getInt("p1_score");
            int player2Score = result.getInt("p2_score");
            // region LocationNames
            JsonNode locationNamesNode = mapper.readTree(result.getString("location_names"));
            String[] locationNames = new String[locationNamesNode.size()];
            for (int i = 0; i < locationNamesNode.size(); i++) {
                locationNames[i] = locationNamesNode.get(i).asText();
            }
            // endregion
            String bracketName = result.getString("bracket_name");
            String bracketOrder = result.getString("bracket_order");
            String setOrder = result.getString("set_order");
            int bestOf = result.getInt("best_of");
            String gameData = result.getString("game_data");
            SmashSets smashSets = new SmashSets(this,
                    game,
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
                    mapper.readValue(gameData, SmashSets.GameData[].class));
            if ((LOG_DETAILS_LEVEL & 0x1) == 0x1) {
                LOGGER.trace("Sets {} built", key);
            } else if ((LOG_DETAILS_LEVEL & 0x2) == 0x2) {
                LOGGER.trace("Sets {} built => {}", key, smashSets.toString().substring(0, 70) + "...");
            } else if ((LOG_DETAILS_LEVEL & 0x3) == 0x3) {
                LOGGER.trace("Sets {} built => {}", key, smashSets);
            }
            sets.add(smashSets);
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Got {} sets of {} for player {} in {}ms", sets.size(), game, playerId, end - start);
        return sets;
    }

    public void invalidatePlayer(SmashGame game) {
        LOGGER.debug("Invalidating players of {}", game);
        cachePlayers.remove(game);
    }

    public static void main(String[] args) throws SQLException {
        SmashBros smash = new SmashBros();
        try {
            List<SmashPlayer> utimatePlayers = smash.getPlayers(SmashGame.ULTIMATE);
            List<SmashPlayer> smashPlayers = smash.getPlayers(SmashGame.MELEE);
            List<SmashTournament> ultimateTournaments = smash.getTournaments(SmashGame.ULTIMATE);
            List<SmashTournament> meleeTournaments = smash.getTournaments(SmashGame.MELEE);
            System.out.println("utimatePlayers[size]: " + utimatePlayers.size());
            System.out.println("smashPlayers[size]: " + smashPlayers.size());
            System.out.println("ultimateTournaments[size]: " + ultimateTournaments.size());
            System.out.println("meleeTournaments[size]: " + meleeTournaments.size());
//        smash.getPlayers(SmashGame.ULTIMATE).stream().filter(smashPlayer -> Arrays.stream(smashPlayer.prefixes()).anyMatch(s -> s.equals("eLine"))).forEach(System.out::println);
            SmashTournament tournament = smash.getTournaments(SmashGame.ULTIMATE).stream().filter(smashTournament -> smashTournament.key().contains("esport-line")).findFirst().orElseThrow();
            System.out.println(tournament);
            HashMap<SmashPlayer, Integer> players = new HashMap<>();
            for (SmashTournament.Placing placing1 : tournament.placings()) {
                players.put(placing1.getPlayer(), placing1.placings());
            }
            List<Map.Entry<SmashPlayer, Integer>> list = new ArrayList<>(players.entrySet());
            list.sort(Map.Entry.comparingByValue());
            for (Map.Entry<SmashPlayer, Integer> placing : list) {
                System.out.println(placing.getValue() + " => " + Arrays.toString(placing.getKey().prefixes()) + " " + placing.getKey().tag());
            }
        } catch (IOException e) {
            if (e.getMessage().contains("403")) {
                System.out.println("You have reached the limit of 60 requests per hour, please wait an hour before retrying");
                System.exit(1);
            } else {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    enum SmashGame {
        MELEE("melee"),
        ULTIMATE("ultimate");

        private final String name;

        SmashGame(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getFileName() {
            return name + "_player_database.zip";
        }

        public static SmashGame getByName(String name) {
            return Arrays.stream(values()).filter(smashGame -> smashGame.getName().equals(name)).findFirst().orElse(null);
        }
    }
}
