package fr.enimaloc.esportline.api.smash;

import fr.enimaloc.esportline.api.github.Github;
import fr.enimaloc.esportline.api.github.LatestGithubRelease;
import fr.enimaloc.esportline.api.smash.sql.PlayerTable;
import fr.enimaloc.esportline.api.smash.sql.SetTable;
import fr.enimaloc.esportline.api.smash.sql.TournamentTable;
import fr.enimaloc.esportline.utils.BenchmarkUtils;
import fr.enimaloc.esportline.utils.SqlUtils;
import fr.enimaloc.esportline.utils.function.ThrowableSupplier;
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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SmashBros {
    public static final Logger LOGGER = LoggerFactory.getLogger(SmashBros.class);
    public static final String GITHUB_ORG = "smashdata";
    public static final String GITHUB_REPO = "ThePlayerDatabase";
    private File smashData = new File("data/smash");
    private LatestGithubRelease release;
    private Map<SmashGame, PlayerTable> playerTables = new HashMap<>();
    private Map<SmashGame, TournamentTable> tournamentTable = new HashMap<>();
    private Map<SmashGame, SetTable> setTable = new HashMap<>();

    public SmashBros() throws SQLException, IOException {
        if (!smashData.exists()) {
            smashData.mkdirs();
        }
        for (SmashGame game : SmashGame.values()) {
            updateConnections(game);
        }
    }

    public Connection getConnection(SmashGame game) throws IOException, SQLException {
        if (release == null || checkUpdate()) {
            update(game);
        }
        File dbFile = new File(smashData, release.tagName() + "_" + game.getDbFileName());
        if (!dbFile.exists()) {
            LOGGER.debug("{} doesn't exist, updating", dbFile);
            update(game);
        }
        LOGGER.debug("Getting connection to {}", dbFile);
        Connection sql = DriverManager.getConnection(JDBC.PREFIX + dbFile.getAbsolutePath());
        LOGGER.debug("Connection to {} got", dbFile);
        return sql;
    }

    public PlayerTable getPlayerTable(SmashGame game) {
        if (!playerTables.containsKey(game)) {
            try {
                updateConnections(game);
            } catch (SQLException | IOException e) {
                LOGGER.error("Error while updating connections", e);
            }
        }
        return playerTables.get(game);
    }

    public TournamentTable getTournamentTable(SmashGame game) {
        if (!tournamentTable.containsKey(game)) {
            try {
                updateConnections(game);
            } catch (SQLException | IOException e) {
                LOGGER.error("Error while updating connections", e);
            }
        }
        return tournamentTable.get(game);
    }

    public SetTable getSetTable(SmashGame game) {
        if (!setTable.containsKey(game)) {
            try {
                updateConnections(game);
            } catch (SQLException | IOException e) {
                LOGGER.error("Error while updating connections", e);
            }
        }
        return setTable.get(game);
    }

    public boolean checkUpdate() {
        try {
            return !release.tagName().equals(Github.getLastRelease(GITHUB_ORG, GITHUB_REPO).tagName());
        } catch (IOException e) {
            LOGGER.error("Error while checking update", e);
            return false;
        }
    }

    public void update(SmashGame game) throws IOException {
        if (release != null && new File(release.tagName() + "_" + game.getDbFileName()).exists() && !checkUpdate()) {
            LOGGER.debug("No update available");
            return;
        }
        LOGGER.debug("Update available");
        LOGGER.debug("Getting release");
        if (!smashData.exists()) {
            smashData.mkdirs();
        }
        release = Github.getLastRelease(GITHUB_ORG, GITHUB_REPO);
        LOGGER.debug("Got release {}", release);
        File dbFile = new File(smashData, release.tagName() + "_" + game.getDbFileName());
        LOGGER.debug("Checking if {} exists", dbFile);
        if (dbFile.exists()) {
            LOGGER.debug("{} exists, deleting", dbFile);
            dbFile.delete();
            LOGGER.debug("{} deleted", dbFile);
        }
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
    }

    public void updateConnections(SmashGame game) throws SQLException, IOException {
        playerTables.put(game, new PlayerTable(this, game));
        tournamentTable.put(game, new TournamentTable(this, game));
        setTable.put(game, new SetTable(this, game));
    }

    public enum SmashGame {
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

        public String getDbFileName() {
            return name + "_player_database.db";
        }

        public static SmashGame getByName(String name) {
            return Arrays.stream(values()).filter(smashGame -> smashGame.getName().equals(name)).findFirst().orElse(null);
        }
    }
}
