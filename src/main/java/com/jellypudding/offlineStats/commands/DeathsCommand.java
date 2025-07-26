package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class DeathsCommand extends BaseStatsCommand {

    public DeathsCommand(OfflineStats plugin) {
        super(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf) {
        String timeText = stats.getDeaths() == 1 ? "time" : "times";
        Component playerName = getPlayerDisplayName(stats);
        Component message = playerName
            .append(Component.text(" has died ", NamedTextColor.YELLOW))
            .append(Component.text(stats.getDeaths(), NamedTextColor.RED))
            .append(Component.text(" " + timeText + ".", NamedTextColor.YELLOW));

        sender.sendMessage(message);
    }
}
