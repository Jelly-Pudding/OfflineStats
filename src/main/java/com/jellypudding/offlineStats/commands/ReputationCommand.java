package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ReputationCommand extends BaseStatsCommand {

    public ReputationCommand(OfflineStats plugin) {
        super(plugin);
    }

    @Override
    protected void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf) {
        Component playerName = getPlayerDisplayName(stats);

        int positiveRep = stats.getPositiveRep();
        int negativeRep = stats.getNegativeRep();
        int netRep = stats.getNetRep();

        String netDisplay = netRep > 0 ? "+" + netRep : (netRep < 0 ? "-" + Math.abs(netRep) : "0");
        NamedTextColor netColor = netRep > 0 ? NamedTextColor.GREEN : (netRep < 0 ? NamedTextColor.RED : NamedTextColor.WHITE);

        Component message = playerName
            .append(Component.text(" has ", NamedTextColor.YELLOW))
            .append(Component.text(netDisplay, netColor))
            .append(Component.text(" reputation (", NamedTextColor.YELLOW))
            .append(Component.text("+" + positiveRep, NamedTextColor.GREEN))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text("-" + negativeRep, NamedTextColor.RED))
            .append(Component.text(").", NamedTextColor.YELLOW));

        sender.sendMessage(message);
    }
}
