package com.jellypudding.offlineStats.database;

import com.jellypudding.offlineStats.OfflineStats;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final OfflineStats plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseManager(OfflineStats plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder() + File.separator + "offlinestats.db";
    }

    public void initialise() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            createTables();

            plugin.getLogger().info("Database initialised successfully.");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialise database!", e);
        }
    }

    private void createTables() throws SQLException {
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                first_seen DATETIME NOT NULL,
                last_seen DATETIME NOT NULL,
                time_played BIGINT DEFAULT 0,
                session_start BIGINT DEFAULT 0,
                kills INTEGER DEFAULT 0,
                deaths INTEGER DEFAULT 0,
                chat_messages INTEGER DEFAULT 0,
                positive_rep INTEGER DEFAULT 0,
                negative_rep INTEGER DEFAULT 0
            );
        """;

        String createMilestonesTable = """
            CREATE TABLE IF NOT EXISTS milestones (
                uuid TEXT NOT NULL,
                milestone_type TEXT NOT NULL,
                milestone_value INTEGER NOT NULL,
                achieved_at DATETIME NOT NULL,
                PRIMARY KEY (uuid, milestone_type, milestone_value)
            );
        """;

        String createRepCooldownsTable = """
            CREATE TABLE IF NOT EXISTS reputation_cooldowns (
                giver_uuid TEXT NOT NULL,
                receiver_uuid TEXT NOT NULL,
                rep_type TEXT NOT NULL,
                last_rep_time BIGINT NOT NULL,
                PRIMARY KEY (giver_uuid, receiver_uuid)
            );
        """;

        try (PreparedStatement stmt1 = connection.prepareStatement(createPlayersTable);
             PreparedStatement stmt2 = connection.prepareStatement(createMilestonesTable);
             PreparedStatement stmt3 = connection.prepareStatement(createRepCooldownsTable)) {

            stmt1.executeUpdate();
            stmt2.executeUpdate();
            stmt3.executeUpdate();
        }

        // New feature so need to add if it's not present in db...
        addColumnIfNotExists("players", "positive_rep", "INTEGER DEFAULT 0");
        addColumnIfNotExists("players", "negative_rep", "INTEGER DEFAULT 0");
    }

    private void addColumnIfNotExists(String table, String column, String type) {
        try {
            String checkQuery = "SELECT " + column + " FROM " + table + " LIMIT 1";
            try (PreparedStatement stmt = connection.prepareStatement(checkQuery)) {
                stmt.executeQuery();
            }
        } catch (SQLException e) {
            try {
                String alterQuery = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
                try (PreparedStatement stmt = connection.prepareStatement(alterQuery)) {
                    stmt.executeUpdate();
                    plugin.getLogger().info("Added column " + column + " to " + table + " table");
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to add column " + column + " to " + table + ": " + ex.getMessage());
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection!", e);
        }
    }

    public void createOrUpdatePlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        String username = player.getName();
        String now = getCurrentTimestamp();

        String selectQuery = "SELECT uuid FROM players WHERE uuid = ?";
        String insertQuery = """
            INSERT INTO players (uuid, username, first_seen, last_seen, session_start) 
            VALUES (?, ?, ?, ?, ?)
        """;
        String updateQuery = "UPDATE players SET username = ?, last_seen = ?, session_start = ? WHERE uuid = ?";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
            selectStmt.setString(1, uuid);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, username);
                    updateStmt.setString(2, now);
                    updateStmt.setLong(3, System.currentTimeMillis());
                    updateStmt.setString(4, uuid);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, uuid);
                    insertStmt.setString(2, username);
                    insertStmt.setString(3, now);
                    insertStmt.setString(4, now);
                    insertStmt.setLong(5, System.currentTimeMillis());
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating/updating player data for " + username, e);
        }
    }

    public void updatePlayerOnQuit(Player player) {
        String uuid = player.getUniqueId().toString();
        String now = getCurrentTimestamp();

        String query = """
            UPDATE players 
            SET last_seen = ?, 
                time_played = time_played + (? - session_start),
                session_start = 0
            WHERE uuid = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, now);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating player quit data for " + player.getName(), e);
        }
    }

    public void incrementKills(UUID playerUuid) {
        String query = "UPDATE players SET kills = kills + 1 WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing kills for " + playerUuid, e);
        }
    }

    public void incrementDeaths(UUID playerUuid) {
        String query = "UPDATE players SET deaths = deaths + 1 WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing deaths for " + playerUuid, e);
        }
    }

    public void incrementChatMessages(UUID playerUuid) {
        String query = "UPDATE players SET chat_messages = chat_messages + 1 WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing chat messages for " + playerUuid, e);
        }
    }

    // Getter methods
    public PlayerStats getPlayerStats(String playerName) {
        UUID playerUuid = com.jellypudding.offlineStats.utils.PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) {
            return null;
        }

        return getPlayerStats(playerUuid);
    }

    public PlayerStats getPlayerStats(UUID playerUuid) {
        String query = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlayerStats(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getString("first_seen"),
                    rs.getString("last_seen"),
                    rs.getLong("time_played"),
                    rs.getLong("session_start"),
                    rs.getInt("kills"),
                    rs.getInt("deaths"),
                    rs.getInt("chat_messages"),
                    rs.getInt("positive_rep"),
                    rs.getInt("negative_rep")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player stats for " + playerUuid, e);
        }
        return null;
    }

    public String getExistingRepType(UUID giverUuid, UUID receiverUuid) {
        String query = "SELECT rep_type FROM reputation_cooldowns WHERE giver_uuid = ? AND receiver_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, giverUuid.toString());
            stmt.setString(2, receiverUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("rep_type");
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting existing rep type", e);
            return null;
        }
    }

    public void giveReputation(UUID giverUuid, UUID receiverUuid, boolean positive) {
        String existingType = getExistingRepType(giverUuid, receiverUuid);

        if (existingType != null) {
            if (existingType.equals("positive")) {
                decrementPositiveRep(receiverUuid);
            } else {
                decrementNegativeRep(receiverUuid);
            }
        }

        if (positive) {
            incrementPositiveRep(receiverUuid);
        } else {
            incrementNegativeRep(receiverUuid);
        }
        updateRepRecord(giverUuid, receiverUuid, positive ? "positive" : "negative");
    }

    private void incrementPositiveRep(UUID playerUuid) {
        String query = "UPDATE players SET positive_rep = positive_rep + 1 WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing positive rep for " + playerUuid, e);
        }
    }

    private void decrementPositiveRep(UUID playerUuid) {
        String query = "UPDATE players SET positive_rep = MAX(0, positive_rep - 1) WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error decrementing positive rep for " + playerUuid, e);
        }
    }

    private void incrementNegativeRep(UUID playerUuid) {
        String query = "UPDATE players SET negative_rep = negative_rep + 1 WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing negative rep for " + playerUuid, e);
        }
    }

    private void decrementNegativeRep(UUID playerUuid) {
        String query = "UPDATE players SET negative_rep = MAX(0, negative_rep - 1) WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error decrementing negative rep for " + playerUuid, e);
        }
    }

    public boolean canGiveReputation(UUID giverUuid, UUID receiverUuid) {
        String query = "SELECT last_rep_time FROM reputation_cooldowns WHERE giver_uuid = ? AND receiver_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, giverUuid.toString());
            stmt.setString(2, receiverUuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long lastRepTime = rs.getLong("last_rep_time");
                long twentyFourHoursMs = 24 * 60 * 60 * 1000L;
                return (System.currentTimeMillis() - lastRepTime) >= twentyFourHoursMs;
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking reputation cooldown", e);
            return false;
        }
    }

    public long getRepCooldownRemaining(UUID giverUuid, UUID receiverUuid) {
        String query = "SELECT last_rep_time FROM reputation_cooldowns WHERE giver_uuid = ? AND receiver_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, giverUuid.toString());
            stmt.setString(2, receiverUuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long lastRepTime = rs.getLong("last_rep_time");
                long twentyFourHoursMs = 24 * 60 * 60 * 1000L;
                long remaining = twentyFourHoursMs - (System.currentTimeMillis() - lastRepTime);
                return Math.max(0, remaining);
            }
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting reputation cooldown remaining", e);
            return 0;
        }
    }

    private void updateRepRecord(UUID giverUuid, UUID receiverUuid, String repType) {
        String query = """
            INSERT INTO reputation_cooldowns (giver_uuid, receiver_uuid, rep_type, last_rep_time) 
            VALUES (?, ?, ?, ?)
            ON CONFLICT(giver_uuid, receiver_uuid) DO UPDATE SET rep_type = ?, last_rep_time = ?
        """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            long now = System.currentTimeMillis();
            stmt.setString(1, giverUuid.toString());
            stmt.setString(2, receiverUuid.toString());
            stmt.setString(3, repType);
            stmt.setLong(4, now);
            stmt.setString(5, repType);
            stmt.setLong(6, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating reputation record", e);
        }
    }

    public boolean hasMilestone(UUID playerUuid, String milestoneType, int milestoneValue) {
        String query = "SELECT 1 FROM milestones WHERE uuid = ? AND milestone_type = ? AND milestone_value = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, milestoneType);
            stmt.setInt(3, milestoneValue);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking milestone for " + playerUuid, e);
            return false;
        }
    }

    public void addMilestone(UUID playerUuid, String milestoneType, int milestoneValue) {
        String query = "INSERT INTO milestones (uuid, milestone_type, milestone_value, achieved_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, milestoneType);
            stmt.setInt(3, milestoneValue);
            stmt.setString(4, getCurrentTimestamp());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding milestone for " + playerUuid, e);
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Calculate current time played for online players
    public long getCurrentTimePlayed(UUID playerUuid) {
        PlayerStats stats = getPlayerStats(playerUuid);
        if (stats == null) return 0;

        long totalTime = stats.getTimePlayed();
        if (stats.getSessionStart() > 0) {
            totalTime += (System.currentTimeMillis() - stats.getSessionStart());
        }
        return totalTime;
    }

    public java.util.List<PlayerStats> getTopPlayersByTimePlayed(int limit) {
        String query = """
            SELECT *,
                   CASE WHEN session_start > 0
                        THEN time_played + (? - session_start)
                        ELSE time_played
                   END as total_time_played
            FROM players
            ORDER BY total_time_played DESC
            LIMIT ?
        """;

        java.util.List<PlayerStats> results = new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(new PlayerStats(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getString("first_seen"),
                    rs.getString("last_seen"),
                    rs.getLong("time_played"),
                    rs.getLong("session_start"),
                    rs.getInt("kills"),
                    rs.getInt("deaths"),
                    rs.getInt("chat_messages"),
                    rs.getInt("positive_rep"),
                    rs.getInt("negative_rep")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting top players by time played", e);
        }
        return results;
    }

    public java.util.List<PlayerStats> getTopPlayersByKills(int limit) {
        String query = "SELECT * FROM players ORDER BY kills DESC LIMIT ?";
        return executeLeaderboardQuery(query, limit);
    }

    public java.util.List<PlayerStats> getTopPlayersByDeaths(int limit) {
        String query = "SELECT * FROM players ORDER BY deaths DESC LIMIT ?";
        return executeLeaderboardQuery(query, limit);
    }

    public java.util.List<PlayerStats> getTopPlayersByChatMessages(int limit) {
        String query = "SELECT * FROM players ORDER BY chat_messages DESC LIMIT ?";
        return executeLeaderboardQuery(query, limit);
    }

    public java.util.List<PlayerStats> getTopPlayersByPositiveRep(int limit) {
        String query = "SELECT * FROM players ORDER BY (positive_rep - negative_rep) DESC LIMIT ?";
        return executeLeaderboardQuery(query, limit);
    }

    public java.util.List<PlayerStats> getTopPlayersByNegativeRep(int limit) {
        String query = "SELECT * FROM players ORDER BY (positive_rep - negative_rep) ASC LIMIT ?";
        return executeLeaderboardQuery(query, limit);
    }

    private java.util.List<PlayerStats> executeLeaderboardQuery(String query, int limit) {
        java.util.List<PlayerStats> results = new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(new PlayerStats(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getString("first_seen"),
                    rs.getString("last_seen"),
                    rs.getLong("time_played"),
                    rs.getLong("session_start"),
                    rs.getInt("kills"),
                    rs.getInt("deaths"),
                    rs.getInt("chat_messages"),
                    rs.getInt("positive_rep"),
                    rs.getInt("negative_rep")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing leaderboard query", e);
        }
        return results;
    }
}
