package com.jellypudding.offlineStats.utils;

import com.jellypudding.offlineStats.OfflineStats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiFarmingManager {

    private final OfflineStats plugin;

    private final Map<UUID, List<Long>> playerDeaths = new ConcurrentHashMap<>();

    // Track player kills with victim information and timestamps
    private final Map<UUID, Map<UUID, List<Long>>> playerKills = new ConcurrentHashMap<>();

    // Configuration values (in milliseconds and count)
    private final long TIME_WINDOW;
    private final int MAX_DEATHS_IN_WINDOW;
    private final int MAX_KILLS_SAME_VICTIM_IN_WINDOW;

    public AntiFarmingManager(OfflineStats plugin) {
        this.plugin = plugin;

        this.TIME_WINDOW = plugin.getConfig().getLong("anti-farming.time-window-minutes", 10) * 60 * 1000;
        this.MAX_DEATHS_IN_WINDOW = plugin.getConfig().getInt("anti-farming.max-deaths-in-window", 20);
        this.MAX_KILLS_SAME_VICTIM_IN_WINDOW = plugin.getConfig().getInt("anti-farming.max-kills-same-victim-in-window", 20);

        plugin.getLogger().info("AntiFarmingManager initialised with " + (TIME_WINDOW / 60000) + " minute window, " + 
                               MAX_DEATHS_IN_WINDOW + " max deaths, " + MAX_KILLS_SAME_VICTIM_IN_WINDOW + " max kills per victim");
    }

    public boolean shouldCountDeath(UUID playerUuid) {
        long currentTime = System.currentTimeMillis();

        List<Long> deaths = playerDeaths.computeIfAbsent(playerUuid, k -> new ArrayList<>());

        deaths.removeIf(timestamp -> currentTime - timestamp > TIME_WINDOW);

        if (deaths.size() >= MAX_DEATHS_IN_WINDOW) {
            plugin.getLogger().info("Death farming detected for player " + playerUuid + 
                                    " - " + deaths.size() + " deaths in the last " + (TIME_WINDOW / 60000) + " minutes");
            return false;
        }

        deaths.add(currentTime);

        return true;
    }

    public boolean shouldCountKill(UUID killerUuid, UUID victimUuid) {
        // If victim is null (NPC), always count it - we only check for player farming
        if (victimUuid == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis();

        Map<UUID, List<Long>> killerMap = playerKills.computeIfAbsent(killerUuid, k -> new ConcurrentHashMap<>());

        List<Long> killsOnVictim = killerMap.computeIfAbsent(victimUuid, k -> new ArrayList<>());

        killsOnVictim.removeIf(timestamp -> currentTime - timestamp > TIME_WINDOW);

        if (killsOnVictim.size() >= MAX_KILLS_SAME_VICTIM_IN_WINDOW) {
            plugin.getLogger().info("Kill farming detected for killer " + killerUuid + 
                                    " against victim " + victimUuid + 
                                    " - " + killsOnVictim.size() + " kills in the last " + (TIME_WINDOW / 60000) + " minutes");
            return false;
        }

        killsOnVictim.add(currentTime);

        return true;
    }

    public void cleanupOldData() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - TIME_WINDOW;

        playerDeaths.entrySet().removeIf(entry -> {
            List<Long> deaths = entry.getValue();
            deaths.removeIf(timestamp -> timestamp < cutoffTime);
            return deaths.isEmpty();
        });

        playerKills.entrySet().removeIf(killerEntry -> {
            Map<UUID, List<Long>> victimMap = killerEntry.getValue();
            victimMap.entrySet().removeIf(victimEntry -> {
                List<Long> kills = victimEntry.getValue();
                kills.removeIf(timestamp -> timestamp < cutoffTime);
                return kills.isEmpty();
            });
            return victimMap.isEmpty();
        });
    }
}
