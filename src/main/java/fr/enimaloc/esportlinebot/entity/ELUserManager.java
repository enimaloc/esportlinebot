package fr.enimaloc.esportlinebot.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ELUserManager {
    private final Connection sql;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private List<ELUser> users = new ArrayList<>();

    public ELUserManager(Connection sql) {
        this.sql = sql;
        this.executor.schedule(this::save, 1, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(this::save));

        try (PreparedStatement statement = sql.prepareStatement("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, guild_id INTEGER, level INTEGER, xp REAL)")) {
            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int count = 0;
        try (PreparedStatement stmt = sql.prepareStatement("SELECT COUNT(*) FROM users;");
             ResultSet resultSet = stmt.executeQuery()) {
            count = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (PreparedStatement stmt = sql.prepareStatement("SELECT * FROM users;");
             ResultSet resultSet = stmt.executeQuery()) {
            while (count > 0 && resultSet.next()) {
                users.add(new ELUser(
                        resultSet.getLong("id"),
                        resultSet.getLong("guild_id"),
                        resultSet.getInt("level"),
                        resultSet.getDouble("xp")));
                count--;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ELUser getOrCreate(long guildId, long userId) {
        if (users.stream().noneMatch(user -> user.getId() == userId && user.getGuildId() == guildId)) {
            try {
                PreparedStatement statement = sql.prepareStatement("INSERT OR IGNORE INTO users (id, guild_id, level, xp) VALUES (?, ?, ?, ?)");
                statement.setLong(1, userId);
                statement.setLong(2, guildId);
                statement.setInt(3, 0);
                statement.setDouble(4, 0);
                statement.execute();
                statement.close();
                users.add(new ELUser(userId, guildId));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return users.stream()
                .filter(user -> user.getId() == userId && user.getGuildId() == guildId)
                .findFirst()
                .orElseThrow();
    }

    public void save() {
        try {
            PreparedStatement statement = sql.prepareStatement("UPDATE users SET level = ?, xp = ? WHERE id = ? AND guild_id = ?");
            for (ELUser user : users) {
                statement.setInt(1, user.getLevel());
                statement.setDouble(2, user.getXp());
                statement.setLong(3, user.getId());
                statement.setLong(4, user.getGuildId());
                statement.addBatch();
            }
            statement.executeBatch();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
