package com.jellypudding.offlineStats.database;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerStats {

    private final UUID uuid;
    private final String username;
    private final String firstSeen;
    private final String lastSeen;
    // milliseconds
    private final long timePlayed;
    // milliseconds, 0 if offline
    private final long sessionStart;
    private final int kills;
    private final int deaths;
    private final int chatMessages;

    public PlayerStats(UUID uuid, String username, String firstSeen, String lastSeen, 
                      long timePlayed, long sessionStart, int kills, int deaths, int chatMessages) {
        this.uuid = uuid;
        this.username = username;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.timePlayed = timePlayed;
        this.sessionStart = sessionStart;
        this.kills = kills;
        this.deaths = deaths;
        this.chatMessages = chatMessages;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getFirstSeen() { return firstSeen; }
    public String getLastSeen() { return lastSeen; }
    public long getTimePlayed() { return timePlayed; }
    public long getSessionStart() { return sessionStart; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getChatMessages() { return chatMessages; }

    public boolean isOnline() {
        return sessionStart > 0;
    }

    public String getFormattedFirstSeen() {
        return formatDate(firstSeen);
    }

    public String getFormattedLastSeen() {
        return formatDate(lastSeen);
    }

    public String getFormattedTimePlayed() {
        long totalMillis = timePlayed;
        if (sessionStart > 0) {
            totalMillis += (System.currentTimeMillis() - sessionStart);
        }
        return formatDuration(totalMillis);
    }

    public long getTimePlayedHours() {
        long totalMillis = timePlayed;
        if (sessionStart > 0) {
            totalMillis += (System.currentTimeMillis() - sessionStart);
        }
        return TimeUnit.MILLISECONDS.toHours(totalMillis);
    }

    private String formatDate(String dateStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatDuration(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;

        if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
