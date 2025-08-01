package com.jellypudding.offlineStats.database;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        return formatDateISO8601(firstSeen);
    }

    public String getFormattedLastSeen() {
        return formatDateISO8601(lastSeen);
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

    private String formatDateISO8601(String dateStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatDuration(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;

        if (hours > 0) {
            String hourText = hours == 1 ? "hour" : "hours";
            String minuteText = minutes == 1 ? "minute" : "minutes";
            return String.format("%d %s, %d %s", hours, hourText, minutes, minuteText);
        } else if (minutes > 0) {
            String minuteText = minutes == 1 ? "minute" : "minutes";
            String secondText = seconds == 1 ? "second" : "seconds";
            return String.format("%d %s, %d %s", minutes, minuteText, seconds, secondText);
        } else {
            String secondText = seconds == 1 ? "second" : "seconds";
            return String.format("%d %s", seconds, secondText);
        }
    }
}
