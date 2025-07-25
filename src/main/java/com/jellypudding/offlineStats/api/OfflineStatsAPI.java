package com.jellypudding.offlineStats.api;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import com.jellypudding.offlineStats.utils.PlayerUtil;
import org.bukkit.Bukkit;

import java.util.UUID;

public class OfflineStatsAPI {

    private final OfflineStats plugin;

    public OfflineStatsAPI(OfflineStats plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the OfflineStatsAPI instance
     * @return OfflineStatsAPI instance or null if plugin not found
     */
    public static OfflineStatsAPI getInstance() {
        org.bukkit.plugin.Plugin offlineStatsPlugin = Bukkit.getPluginManager().getPlugin("OfflineStats");
        if (offlineStatsPlugin instanceof OfflineStats && offlineStatsPlugin.isEnabled()) {
            return ((OfflineStats) offlineStatsPlugin).getAPI();
        }
        return null;
    }

    /**
     * Check if OfflineStats is loaded and ready to use
     * @return true if ready, false otherwise
     */
    public static boolean isReady() {
        return getInstance() != null;
    }

    /**
     * Get player statistics by player name
     * @param playerName The player's name
     * @return PlayerStats object or null if not found
     */
    public PlayerStats getPlayerStats(String playerName) {
        return plugin.getDatabaseManager().getPlayerStats(playerName);
    }

    /**
     * Get player statistics by UUID
     * @param playerUuid The player's UUID
     * @return PlayerStats object or null if not found
     */
    public PlayerStats getPlayerStats(UUID playerUuid) {
        return plugin.getDatabaseManager().getPlayerStats(playerUuid);
    }

    /**
     * Get a player's first seen date
     * @param playerName The player's name
     * @return Formatted first seen date or null if player not found
     */
    public String getPlayerFirstSeen(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return null;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getFormattedFirstSeen() : null;
    }

    /**
     * Get a player's last seen date
     * @param playerName The player's name
     * @return Formatted last seen date or null if player not found
     */
    public String getPlayerLastSeen(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return null;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getFormattedLastSeen() : null;
    }

    /**
     * Get a player's time played
     * @param playerName The player's name
     * @return Formatted time played or null if player not found
     */
    public String getPlayerTimePlayed(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return null;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getFormattedTimePlayed() : null;
    }

    /**
     * Get a player's time played in hours
     * @param playerName The player's name
     * @return Hours played or 0 if player not found
     */
    public long getPlayerTimePlayedHours(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return 0;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getTimePlayedHours() : 0;
    }

    /**
     * Get a player's kill count
     * @param playerName The player's name
     * @return Kill count or 0 if player not found
     */
    public int getPlayerKills(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return 0;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getKills() : 0;
    }

    /**
     * Get a player's death count
     * @param playerName The player's name
     * @return Death count or 0 if player not found
     */
    public int getPlayerDeaths(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return 0;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getDeaths() : 0;
    }

    /**
     * Get a player's chat message count
     * @param playerName The player's name
     * @return Chat message count or 0 if player not found
     */
    public int getPlayerChatMessages(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return 0;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null ? stats.getChatMessages() : 0;
    }

    /**
     * Check if a player is currently online
     * @param playerName The player's name
     * @return true if online, false otherwise
     */
    public boolean isPlayerOnline(String playerName) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) return false;
        PlayerStats stats = getPlayerStats(playerUuid);
        return stats != null && stats.isOnline();
    }

    /**
     * Get formatted statistics for Discord commands
     * @param playerName The player's name
     * @param statType The type of stat (firstseen, lastseen, timeplayed, kills, deaths, chatter)
     * @return Formatted stat string or error message
     */
    public String getFormattedStat(String playerName, String statType) {
        UUID playerUuid = PlayerUtil.getPlayerUUID(playerName);
        if (playerUuid == null) {
            return "Player '" + playerName + "' not found.";
        }

        PlayerStats stats = getPlayerStats(playerUuid);
        if (stats == null) {
            return "Player '" + playerName + "' has never joined the server!";
        }

        return switch (statType.toLowerCase()) {
            case "firstseen" -> stats.getUsername() + " first joined on " + stats.getFormattedFirstSeen();
            case "lastseen" -> stats.isOnline() ? 
                stats.getUsername() + " is currently online." : 
                stats.getUsername() + " was last seen on " + stats.getFormattedLastSeen();
            case "timeplayed" -> stats.getUsername() + " has played for " + stats.getFormattedTimePlayed();
            case "kills" -> stats.getUsername() + " has " + stats.getKills() + " kills.";
            case "deaths" -> stats.getUsername() + " has died " + stats.getDeaths() + " times.";
            case "chatter" -> stats.getUsername() + " has sent " + stats.getChatMessages() + " chat messages.";
            default -> "Unknown stat type: " + statType;
        };
    }
}
