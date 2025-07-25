package com.jellypudding.offlineStats.listeners;

import com.jellypudding.offlineStats.OfflineStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStatsListener implements Listener {

    private final OfflineStats plugin;

    public PlayerStatsListener(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getDatabaseManager().createOrUpdatePlayer(player);

        plugin.getMilestoneManager().checkTimePlayedMilestones(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getDatabaseManager().updatePlayerOnQuit(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        plugin.getDatabaseManager().incrementDeaths(player.getUniqueId());

        plugin.getMilestoneManager().checkDeathMilestones(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player killer) {
            plugin.getDatabaseManager().incrementKills(killer.getUniqueId());

            plugin.getMilestoneManager().checkKillMilestones(killer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        plugin.getDatabaseManager().incrementChatMessages(player.getUniqueId());
    }
}
