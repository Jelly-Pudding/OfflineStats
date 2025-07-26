package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class LastSeenCommand extends BaseStatsCommand {

    public LastSeenCommand(OfflineStats plugin) {
        super(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf) {
        Component playerName = getPlayerDisplayName(stats);
        Component message;
        if (stats.isOnline()) {
            message = playerName
                .append(Component.text(" is currently online!", NamedTextColor.YELLOW));
        } else {
            message = playerName
                .append(Component.text(" was last seen on ", NamedTextColor.YELLOW))
                .append(Component.text(stats.getFormattedLastSeen(), NamedTextColor.GREEN));
        }
        sender.sendMessage(message);
    }
}
