package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ChatterCommand extends BaseStatsCommand {

    public ChatterCommand(OfflineStats plugin) {
        super(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf) {
        Component message = Component.text(stats.getUsername(), NamedTextColor.GOLD)
            .append(Component.text(" has sent ", NamedTextColor.YELLOW))
            .append(Component.text(stats.getChatMessages(), NamedTextColor.GREEN))
            .append(Component.text(" chat messages", NamedTextColor.YELLOW));

        sender.sendMessage(message);
    }
}
