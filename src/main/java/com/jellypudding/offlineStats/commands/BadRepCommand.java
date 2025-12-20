package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import com.jellypudding.offlineStats.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BadRepCommand implements CommandExecutor {

    private final OfflineStats plugin;

    public BadRepCommand(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can give reputation!", NamedTextColor.RED));
            return true;
        }

        Player giver = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /badrep <player>", NamedTextColor.YELLOW));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = PlayerUtil.getPlayerUUID(targetName);

        if (targetUuid == null) {
            sender.sendMessage(Component.text("Player '", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW))
                .append(Component.text("' not found.", NamedTextColor.RED)));
            return true;
        }

        if (giver.getUniqueId().equals(targetUuid)) {
            sender.sendMessage(Component.text("I am sorry you feel that way, but you can't give negative reputation to yourself.", NamedTextColor.RED));
            return true;
        }

        PlayerStats targetStats = plugin.getDatabaseManager().getPlayerStats(targetUuid);
        if (targetStats == null) {
            sender.sendMessage(Component.text("Player '", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.YELLOW))
                .append(Component.text("' has never joined the server.", NamedTextColor.RED)));
            return true;
        }

        String existingRepType = plugin.getDatabaseManager().getExistingRepType(giver.getUniqueId(), targetUuid);
        if ("negative".equals(existingRepType)) {
            sender.sendMessage(Component.text("You have already given negative reputation to this player.", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getDatabaseManager().canGiveReputation(giver.getUniqueId(), targetUuid)) {
            long remaining = plugin.getDatabaseManager().getRepCooldownRemaining(giver.getUniqueId(), targetUuid);
            String timeRemaining = formatDuration(remaining);
            sender.sendMessage(Component.text("You must wait ", NamedTextColor.RED)
                .append(Component.text(timeRemaining, NamedTextColor.YELLOW))
                .append(Component.text(" before changing your reputation for this player.", NamedTextColor.RED)));
            return true;
        }

        plugin.getDatabaseManager().giveReputation(giver.getUniqueId(), targetUuid, false);

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        Component targetDisplayName = targetPlayer != null 
            ? targetPlayer.displayName() 
            : PlayerUtil.getPlayerDisplayName(targetStats.getUsername(), targetUuid);
        Component giverDisplayName = giver.displayName();

        Component message = giverDisplayName
            .append(Component.text(" gave negative reputation to ", NamedTextColor.RED))
            .append(targetDisplayName)
            .append(Component.text(".", NamedTextColor.RED));
        Bukkit.getServer().broadcast(message);

        String discordMessage = giver.getName() + " gave negative reputation to " + targetStats.getUsername() + ".";
        plugin.getDiscordUtil().sendMessage("Reputation", discordMessage, Color.RED);

        plugin.getMilestoneManager().checkReputationMilestones(targetUuid);

        return true;
    }

    private String formatDuration(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;

        if (hours > 0) {
            String hourText = hours == 1 ? "hour" : "hours";
            String minuteText = minutes == 1 ? "minute" : "minutes";
            return hours + " " + hourText + ", " + minutes + " " + minuteText;
        } else {
            String minuteText = minutes == 1 ? "minute" : "minutes";
            return minutes + " " + minuteText;
        }
    }
}

