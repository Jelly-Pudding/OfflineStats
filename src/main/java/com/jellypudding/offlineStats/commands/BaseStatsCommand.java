package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.jellypudding.offlineStats.utils.PlayerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class BaseStatsCommand implements CommandExecutor {

    protected final OfflineStats plugin;

    public BaseStatsCommand(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetPlayerUuid;
        String targetPlayerName;
        boolean isSelf = false;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player name!", NamedTextColor.RED));
                return true;
            }
            Player senderPlayer = (Player) sender;
            targetPlayerUuid = senderPlayer.getUniqueId();
            targetPlayerName = senderPlayer.getName();
            isSelf = true;
        } else {
            targetPlayerName = args[0];
            targetPlayerUuid = PlayerUtil.getPlayerUUID(targetPlayerName);

            if (targetPlayerUuid == null) {
                sender.sendMessage(Component.text("Player '", NamedTextColor.RED)
                    .append(Component.text(targetPlayerName, NamedTextColor.YELLOW))
                    .append(Component.text("' not found.", NamedTextColor.RED)));
                return true;
            }

            targetPlayerName = PlayerUtil.getExactPlayerName(targetPlayerName);

            if (sender instanceof Player senderPlayer) {
                isSelf = senderPlayer.getUniqueId().equals(targetPlayerUuid);
            }
        }

        PlayerStats stats = plugin.getDatabaseManager().getPlayerStats(targetPlayerUuid);
        if (stats == null) {
            sender.sendMessage(Component.text("Player '", NamedTextColor.RED)
                .append(Component.text(targetPlayerName, NamedTextColor.YELLOW))
                .append(Component.text("' has never joined the server.", NamedTextColor.RED)));
            return true;
        }

        executeCommand(sender, stats, isSelf);
        return true;
    }

    protected abstract void executeCommand(CommandSender sender, PlayerStats stats, boolean isSelf);
}
