package com.jellypudding.offlineStats.listeners;

import com.jellypudding.offlineStats.OfflineStats;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CombatLogListener implements Listener {

    private final OfflineStats plugin;
    private final NamespacedKey battleLockKey;

    public CombatLogListener(OfflineStats plugin) {
        this.plugin = plugin;
        this.battleLockKey = new NamespacedKey("battlelock", "combat_log_player_id");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatLogNpcKill(EntityDamageByEntityEvent event) {
        // Check if the entity being damaged is a Villager (NPC) and will die from this damage.
        if (!(event.getEntity() instanceof Villager npc) || event.getFinalDamage() < npc.getHealth()) {
            return;
        }

        // Check if this is a BattleLock combat log NPC
        if (!npc.getPersistentDataContainer().has(battleLockKey, PersistentDataType.STRING)) {
            return;
        }

        // Extract the original player's UUID from the persistent data
        String playerUuidString = npc.getPersistentDataContainer().get(battleLockKey, PersistentDataType.STRING);
        UUID originalPlayerUuid = null;
        try {
            originalPlayerUuid = UUID.fromString(playerUuidString);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse UUID from combat log NPC persistent data: " + e.getMessage());
            return;
        }

        Player killer = null;
        Entity damager = event.getDamager();

        if (damager instanceof Player) {
            killer = (Player) damager;
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                killer = shooter;
            }
        }

        if (originalPlayerUuid != null && plugin.getAntiFarmingManager().shouldCountDeath(originalPlayerUuid)) {
            plugin.getDatabaseManager().incrementDeaths(originalPlayerUuid);
            plugin.getLogger().info("Combat log NPC death counted for original player " + originalPlayerUuid + " - death count incremented");
        }

        // Only count the kill if there was a player killer
        if (killer != null && originalPlayerUuid != null) {
            if (plugin.getAntiFarmingManager().shouldCountKill(killer.getUniqueId(), originalPlayerUuid)) {
                plugin.getDatabaseManager().incrementKills(killer.getUniqueId());
                plugin.getMilestoneManager().checkKillMilestones(killer);
                
                plugin.getLogger().info("Player " + killer.getName() + " killed a combat log NPC - kill count incremented");
            }
        }
    }
}
