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

        Player killer = null;
        Entity damager = event.getDamager();

        if (damager instanceof Player) {
            killer = (Player) damager;
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                killer = shooter;
            }
        }

        if (killer != null) {
            plugin.getDatabaseManager().incrementKills(killer.getUniqueId());
            plugin.getLogger().info("Player " + killer.getName() + " killed a combat log NPC - kill count incremented");

            plugin.getMilestoneManager().checkKillMilestones(killer);
        }
    }
}
