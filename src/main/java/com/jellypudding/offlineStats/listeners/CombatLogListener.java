package com.jellypudding.offlineStats.listeners;

import com.jellypudding.offlineStats.OfflineStats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.UUID;

public class CombatLogListener implements Listener {

    private final OfflineStats plugin;
    private final String battleLockMetaKey = "BattleLock_CombatLog";

    public CombatLogListener(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatLogNpcKill(EntityDamageByEntityEvent event) {
        // Check if the entity being damaged is a Villager (NPC) and will die from this damage.
        if (!(event.getEntity() instanceof Villager npc) || event.getFinalDamage() < npc.getHealth()) {
            return;
        }

        // Check if this NPC has BattleLock combat log metadata.
        List<MetadataValue> metadata = npc.getMetadata(battleLockMetaKey);
        if (metadata.isEmpty()) {
            return;
        }

        // Extract the original player's UUID from the metadata
        UUID originalPlayerUuid = null;
        try {
            String uuidString = metadata.get(0).asString();
            originalPlayerUuid = UUID.fromString(uuidString);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse UUID from combat log NPC metadata: " + e.getMessage());
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
