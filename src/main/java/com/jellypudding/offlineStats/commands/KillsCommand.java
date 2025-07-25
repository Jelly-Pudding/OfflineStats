package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class KillsCommand extends BaseStatsCommand {

    public KillsCommand(OfflineStats plugin) {
        super(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf) {
        Component message = Component.text(stats.getUsername(), NamedTextColor.GOLD)
            .append(Component.text(" has ", NamedTextColor.YELLOW))
            .append(Component.text(stats.getKills(), NamedTextColor.RED))
            .append(Component.text(" kills", NamedTextColor.YELLOW));

        sender.sendMessage(message);
    }
}
