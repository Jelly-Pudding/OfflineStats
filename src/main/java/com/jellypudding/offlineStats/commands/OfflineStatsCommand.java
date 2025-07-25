package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class OfflineStatsCommand implements CommandExecutor {

    private final OfflineStats plugin;

    public OfflineStatsCommand(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("offlinestats.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /offlinestats reload", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadConfig();
                sender.sendMessage(Component.text("OfflineStats configuration reloaded successfully.", NamedTextColor.GREEN));
                plugin.getLogger().info(sender.getName() + " reloaded the OfflineStats configuration.");
            } catch (Exception e) {
                sender.sendMessage(Component.text("Error reloading configuration: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            }
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /offlinestats reload", NamedTextColor.RED));
        return true;
    }
}
