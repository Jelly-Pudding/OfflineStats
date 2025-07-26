package com.jellypudding.offlineStats.milestones;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Level;
import java.util.UUID;

public class MilestoneManager {

    private final OfflineStats plugin;

    public MilestoneManager(OfflineStats plugin) {
        this.plugin = plugin;
    }

    public void checkTimePlayedMilestones(Player player) {
        if (!plugin.getConfig().getBoolean("milestones.timeplayed.enabled", true)) {
            return;
        }

        PlayerStats stats = plugin.getDatabaseManager().getPlayerStats(player.getUniqueId());
        if (stats == null) return;

        long hoursPlayed = stats.getTimePlayedHours();
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("milestones.timeplayed.rewards");

        if (rewards == null) return;

        boolean announcedThisCheck = false;

        // Check for milestone rewards first.
        for (String key : rewards.getKeys(false)) {
            try {
                int milestone = Integer.parseInt(key);

                if (hoursPlayed >= milestone && !plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "timeplayed", milestone)) {
                    awardTimePlayedMilestone(player, milestone);
                    announcedThisCheck = true;
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid milestone key in timeplayed rewards: " + key);
            }
        }

        // Check for 1000-hour announcements (every 1000 hours starting from 1000)
        if (hoursPlayed >= 1000) {
            long thousandHourMilestones = hoursPlayed / 1000;
            int currentThousandMilestone = (int) (thousandHourMilestones * 1000);

            if (!plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "timeplayed_1000h", currentThousandMilestone)) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "timeplayed_1000h", currentThousandMilestone);

                if (!announcedThisCheck) {
                    sendTimePlayedAnnouncement(player, currentThousandMilestone, 0);
                }
            }
        }
    }

    public void checkKillMilestones(Player player) {
        if (!plugin.getConfig().getBoolean("milestones.kills.enabled", true)) {
            return;
        }

        PlayerStats stats = plugin.getDatabaseManager().getPlayerStats(player.getUniqueId());
        if (stats == null) return;

        int kills = stats.getKills();
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("milestones.kills.rewards");

        if (rewards == null) return;

        boolean announcedThisCheck = false;

        // Check for milestone rewards first
        for (String key : rewards.getKeys(false)) {
            try {
                int milestone = Integer.parseInt(key);

                if (kills >= milestone && !plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "kills", milestone)) {
                    awardKillMilestone(player, milestone);
                    announcedThisCheck = true; // We announced for the milestone reward
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid milestone key in kills rewards: " + key);
            }
        }

        if (kills >= 1000) {
            long thousandKillMilestones = kills / 1000;
            int currentThousandMilestone = (int) (thousandKillMilestones * 1000);

            if (!plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "kills_1000", currentThousandMilestone)) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "kills_1000", currentThousandMilestone);

                if (!announcedThisCheck) {
                    sendKillAnnouncement(player, currentThousandMilestone, 0);
                }
            }
        }
    }

    public void checkDeathMilestones(Player player) {
        if (!plugin.getConfig().getBoolean("milestones.deaths.enabled", true)) {
            return;
        }

        PlayerStats stats = plugin.getDatabaseManager().getPlayerStats(player.getUniqueId());
        if (stats == null) return;

        int deaths = stats.getDeaths();
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("milestones.deaths.rewards");

        if (rewards == null) return;

        boolean announcedThisCheck = false;

        for (String key : rewards.getKeys(false)) {
            try {
                int milestone = Integer.parseInt(key);

                if (deaths >= milestone && !plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "deaths", milestone)) {
                    awardDeathMilestone(player, milestone);
                    announcedThisCheck = true;
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid milestone key in deaths rewards: " + key);
            }
        }

        if (deaths >= 1000) {
            long thousandDeathMilestones = deaths / 1000;
            int currentThousandMilestone = (int) (thousandDeathMilestones * 1000);

            if (!plugin.getDatabaseManager().hasMilestone(player.getUniqueId(), "deaths_1000", currentThousandMilestone)) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "deaths_1000", currentThousandMilestone);

                if (!announcedThisCheck) {
                    sendDeathAnnouncement(player, currentThousandMilestone, 0);
                }
            }
        }
    }

    private void awardTimePlayedMilestone(Player player, int hoursPlayed) {
        try {
            // Get reward amount from config
            int homeSlots = plugin.getConfig().getInt("milestones.timeplayed.rewards." + hoursPlayed + ".home_slots", 1);
            
            if (plugin.isSimpleHomeEnabled()) {
                try {
                    org.bukkit.plugin.Plugin simpleHomePlugin = Bukkit.getPluginManager().getPlugin("SimpleHome");
                    if (simpleHomePlugin != null && simpleHomePlugin.isEnabled()) {
                        java.lang.reflect.Method increaseHomeLimit = simpleHomePlugin.getClass().getMethod("increaseHomeLimit", UUID.class);
                        increaseHomeLimit.invoke(simpleHomePlugin, player.getUniqueId());
                        plugin.getLogger().info("Awarded " + homeSlots + " home slot" + (homeSlots == 1 ? "" : "s") + " to " + player.getName() + " for " + hoursPlayed + " hours played");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to award home slots: " + e.getMessage());
                }
            }

            plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "timeplayed", hoursPlayed);

            if (hoursPlayed >= 1000 && hoursPlayed % 1000 == 0) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "timeplayed_1000h", hoursPlayed);
            }

            sendTimePlayedAnnouncement(player, hoursPlayed, homeSlots);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error awarding timeplayed milestone to " + player.getName(), e);
        }
    }

    private void awardKillMilestone(Player player, int kills) {
        try {
            // Get reward amount from config
            int maxHearts = plugin.getConfig().getInt("milestones.kills.rewards." + kills + ".max_hearts", 1);
            
            if (plugin.isSimpleLifestealEnabled()) {
                try {
                    org.bukkit.plugin.Plugin simpleLifestealPlugin = Bukkit.getPluginManager().getPlugin("SimpleLifesteal");
                    if (simpleLifestealPlugin != null && simpleLifestealPlugin.isEnabled()) {
                        java.lang.reflect.Method increasePlayerMaxHearts = simpleLifestealPlugin.getClass().getMethod("increasePlayerMaxHearts", UUID.class, int.class);
                        increasePlayerMaxHearts.invoke(simpleLifestealPlugin, player.getUniqueId(), maxHearts);
                        plugin.getLogger().info("Awarded " + maxHearts + " max heart" + (maxHearts == 1 ? "" : "s") + " to " + player.getName() + " for " + kills + " kills");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to award max hearts: " + e.getMessage());
                }
            }

            plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "kills", kills);

            if (kills >= 1000 && kills % 1000 == 0) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "kills_1000", kills);
            }

            sendKillAnnouncement(player, kills, maxHearts);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error awarding kills milestone to " + player.getName(), e);
        }
    }

    private void awardDeathMilestone(Player player, int deaths) {
        try {
            if (plugin.isSimpleVoteEnabled()) {
                int tokens = plugin.getConfig().getInt("milestones.deaths.rewards." + deaths + ".tokens", 5);

                try {
                    org.bukkit.plugin.Plugin simpleVotePlugin = Bukkit.getPluginManager().getPlugin("SimpleVote");
                    if (simpleVotePlugin != null && simpleVotePlugin.isEnabled()) {
                        java.lang.reflect.Method getTokenManager = simpleVotePlugin.getClass().getMethod("getTokenManager");
                        Object tokenManager = getTokenManager.invoke(simpleVotePlugin);
                        java.lang.reflect.Method addTokens = tokenManager.getClass().getMethod("addTokens", UUID.class, int.class);
                        addTokens.invoke(tokenManager, player.getUniqueId(), tokens);
                        plugin.getLogger().info("Awarded " + tokens + " tokens to " + player.getName() + " for " + deaths + " deaths");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to award tokens: " + e.getMessage());
                }
            }

            plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "deaths", deaths);

            if (deaths >= 1000 && deaths % 1000 == 0) {
                plugin.getDatabaseManager().addMilestone(player.getUniqueId(), "deaths_1000", deaths);
            }

            sendDeathAnnouncement(player, deaths, plugin.getConfig().getInt("milestones.deaths.rewards." + deaths + ".tokens", 5));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error awarding deaths milestone to " + player.getName(), e);
        }
    }

    private void sendTimePlayedAnnouncement(Player player, int hours, int homeSlots) {
        Component playerName = player.displayName();
        Component message;
        if (homeSlots > 0) {
            String slotText = homeSlots == 1 ? "home slot" : "home slots";
            String hourText = hours == 1 ? "hour" : "hours";
            message = playerName
                .append(Component.text(" has reached ", NamedTextColor.YELLOW))
                .append(Component.text(hours + " " + hourText, NamedTextColor.GREEN))
                .append(Component.text(" of playtime and received ", NamedTextColor.YELLOW))
                .append(Component.text("+" + homeSlots + " " + slotText + ".", NamedTextColor.AQUA));
        } else {
            String hourText = hours == 1 ? "hour" : "hours";
            message = playerName
                .append(Component.text(" has reached ", NamedTextColor.YELLOW))
                .append(Component.text(hours + " " + hourText, NamedTextColor.GREEN))
                .append(Component.text(" of playtime.", NamedTextColor.YELLOW));
        }

        Bukkit.getServer().broadcast(message);

        if (plugin.isDiscordRelayEnabled()) {
            String discordMessage;
            if (homeSlots > 0) {
                String slotText = homeSlots == 1 ? "home slot" : "home slots";
                String hourText = hours == 1 ? "hour" : "hours";
                discordMessage = player.getName() + " has reached " + hours + " " + hourText + " of playtime and received +" + homeSlots + " " + slotText + ".";
            } else {
                String hourText = hours == 1 ? "hour" : "hours";
                discordMessage = player.getName() + " has reached " + hours + " " + hourText + " of playtime.";
            }

            sendDiscordMessage(discordMessage);
        }
    }

    private void sendKillAnnouncement(Player player, int kills, int hearts) {
        String heartText = hearts == 1 ? "max heart" : "max hearts";
        String killText = kills == 1 ? "kill" : "kills";
        Component playerName = player.displayName();
        Component message = playerName
            .append(Component.text(" has reached ", NamedTextColor.YELLOW))
            .append(Component.text(kills + " " + killText, NamedTextColor.RED))
            .append(Component.text(" and received ", NamedTextColor.YELLOW))
            .append(Component.text("+" + hearts + " " + heartText + ".", NamedTextColor.DARK_RED));

        Bukkit.getServer().broadcast(message);

        if (plugin.isDiscordRelayEnabled()) {
            String discordMessage = player.getName() + " has reached " + kills + " " + killText + " and received +" + hearts + " " + heartText + ".";
            sendDiscordMessage(discordMessage);
        }
    }

    private void sendDeathAnnouncement(Player player, int deaths, int tokens) {
        String timeText = deaths == 1 ? "time" : "times";
        String tokenText = tokens == 1 ? "token" : "tokens";
        Component playerName = player.displayName();
        Component message = playerName
            .append(Component.text(" has died ", NamedTextColor.YELLOW))
            .append(Component.text(deaths + " " + timeText, NamedTextColor.RED))
            .append(Component.text(" and received ", NamedTextColor.YELLOW))
            .append(Component.text(tokens + " " + tokenText + " for their trouble.", NamedTextColor.GOLD));

        Bukkit.getServer().broadcast(message);

        if (plugin.isDiscordRelayEnabled()) {
            String discordMessage = player.getName() + " has died " + deaths + " " + timeText + " and received " + tokens + " " + tokenText + " for their trouble.";
            sendDiscordMessage(discordMessage);
        }
    }

    private void sendDiscordMessage(String message) {
        try {
            Class<?> discordRelayAPI = Class.forName("com.jellypudding.discordRelay.DiscordRelayAPI");
            java.lang.reflect.Method isReady = discordRelayAPI.getMethod("isReady");
            java.lang.reflect.Method sendCustomMessage = discordRelayAPI.getMethod("sendCustomMessage", String.class);

            if ((Boolean) isReady.invoke(null)) {
                sendCustomMessage.invoke(null, message);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord announcement: " + e.getMessage());
        }
    }
}
