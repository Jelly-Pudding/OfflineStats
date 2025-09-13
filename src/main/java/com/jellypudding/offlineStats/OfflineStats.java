package com.jellypudding.offlineStats;

import com.jellypudding.offlineStats.api.OfflineStatsAPI;
import com.jellypudding.offlineStats.commands.*;
import com.jellypudding.offlineStats.listeners.CombatLogListener;
import com.jellypudding.offlineStats.database.DatabaseManager;
import com.jellypudding.offlineStats.listeners.PlayerStatsListener;
import com.jellypudding.offlineStats.milestones.MilestoneManager;
import com.jellypudding.offlineStats.utils.AntiFarmingManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class OfflineStats extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MilestoneManager milestoneManager;
    private OfflineStatsAPI api;
    private AntiFarmingManager antiFarmingManager;
    private BukkitTask cleanupTask;

    // Plugin integrations
    private boolean simpleHomeEnabled = false;
    private boolean simpleLifestealEnabled = false;
    private boolean simpleVoteEnabled = false;
    private boolean discordRelayEnabled = false;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialise database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialise();

        // Check for plugin integrations
        checkPluginIntegrations();

        // Initialise milestone manager
        milestoneManager = new MilestoneManager(this);

        // Initialise anti-farming manager
        antiFarmingManager = new AntiFarmingManager(this);

        // Start cleanup task for anti-farming data (runs every 5 minutes)
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, 
            antiFarmingManager::cleanupOldData, 
            20L * 60 * 5,
            20L * 60 * 5
        );

        // Initialise API
        api = new OfflineStatsAPI(this);

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        getLogger().info("OfflineStats plugin has been enabled.");
        getLogger().info("Plugin integrations: SimpleHome=" + simpleHomeEnabled + 
                        ", SimpleLifesteal=" + simpleLifestealEnabled + 
                        ", SimpleVote=" + simpleVoteEnabled + 
                        ", DiscordRelay=" + discordRelayEnabled);
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                databaseManager.updatePlayerOnQuit(player);
            } catch (Exception e) {
                getLogger().warning("Failed to save data for " + player.getName() + " during shutdown: " + e.getMessage());
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("OfflineStats plugin has been disabled.");
    }

    private void checkPluginIntegrations() {
        // Check SimpleHome
        if (Bukkit.getPluginManager().getPlugin("SimpleHome") != null) {
            simpleHomeEnabled = true;
            getLogger().info("SimpleHome integration enabled!");
        } else {
            getLogger().warning("SimpleHome not found - home slot rewards will be disabled");
        }

        // Check SimpleLifesteal
        if (Bukkit.getPluginManager().getPlugin("SimpleLifesteal") != null) {
            simpleLifestealEnabled = true;
            getLogger().info("SimpleLifesteal integration enabled!");
        } else {
            getLogger().warning("SimpleLifesteal not found - heart rewards will be disabled");
        }

        // Check SimpleVote
        if (Bukkit.getPluginManager().getPlugin("SimpleVote") != null) {
            simpleVoteEnabled = true;
            getLogger().info("SimpleVote integration enabled!");
        } else {
            getLogger().warning("SimpleVote not found - token rewards will be disabled");
        }

        // Check DiscordRelay
        if (Bukkit.getPluginManager().getPlugin("DiscordRelay") != null) {
            discordRelayEnabled = true;
            getLogger().info("DiscordRelay integration enabled!");
        } else {
            getLogger().warning("DiscordRelay not found - Discord announcements will be disabled");
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerStatsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatLogListener(this), this);
    }

    private void registerCommands() {
        getCommand("firstseen").setExecutor(new FirstSeenCommand(this));
        getCommand("lastseen").setExecutor(new LastSeenCommand(this));
        getCommand("timeplayed").setExecutor(new TimePlayedCommand(this));
        getCommand("kills").setExecutor(new KillsCommand(this));
        getCommand("deaths").setExecutor(new DeathsCommand(this));
        getCommand("chatter").setExecutor(new ChatterCommand(this));
        getCommand("offlinestats").setExecutor(new OfflineStatsCommand(this));
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MilestoneManager getMilestoneManager() {
        return milestoneManager;
    }

    public OfflineStatsAPI getAPI() {
        return api;
    }

    public boolean isSimpleHomeEnabled() {
        return simpleHomeEnabled;
    }

    public boolean isSimpleLifestealEnabled() {
        return simpleLifestealEnabled;
    }

    public boolean isSimpleVoteEnabled() {
        return simpleVoteEnabled;
    }

    public boolean isDiscordRelayEnabled() {
        return discordRelayEnabled;
    }

    public AntiFarmingManager getAntiFarmingManager() {
        return antiFarmingManager;
    }
}
