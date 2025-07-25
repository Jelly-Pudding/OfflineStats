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
                chat_messages INTEGER DEFAULT 0
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

        try (PreparedStatement stmt1 = connection.prepareStatement(createPlayersTable);
             PreparedStatement stmt2 = connection.prepareStatement(createMilestonesTable)) {

            stmt1.executeUpdate();
            stmt2.executeUpdate();
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
                    rs.getInt("chat_messages")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player stats for " + playerUuid, e);
        }
        return null;
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
}
