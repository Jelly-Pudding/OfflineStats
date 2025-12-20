package com.jellypudding.offlineStats.commands;

import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.database.PlayerStats;
import com.jellypudding.offlineStats.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final OfflineStats plugin;
    private static final int TOP_PLAYERS = 10;

    public LeaderboardCommand(OfflineStats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("offlinestats.leaderboard")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        String category = "timeplayed";

        // Parse arguments
        if (args.length >= 1) {
            category = args[0].toLowerCase();
        }

        if (!isValidCategory(category)) {
            sender.sendMessage(Component.text("Invalid category! Valid categories: timeplayed, kills, deaths, chatter, loved, hated", NamedTextColor.RED));
            return true;
        }

        List<PlayerStats> topPlayers = getLeaderboardData(category);

        if (topPlayers.isEmpty()) {
            sender.sendMessage(Component.text("No players found for this category.", NamedTextColor.YELLOW));
            return true;
        }

        displayLeaderboard(sender, category, topPlayers);
        return true;
    }

    private boolean isValidCategory(String category) {
        return Arrays.asList("timeplayed", "kills", "deaths", "chatter", "loved", "hated").contains(category);
    }

    private List<PlayerStats> getLeaderboardData(String category) {
        switch (category) {
            case "timeplayed":
                return plugin.getDatabaseManager().getTopPlayersByTimePlayed(TOP_PLAYERS);
            case "kills":
                return plugin.getDatabaseManager().getTopPlayersByKills(TOP_PLAYERS);
            case "deaths":
                return plugin.getDatabaseManager().getTopPlayersByDeaths(TOP_PLAYERS);
            case "chatter":
                return plugin.getDatabaseManager().getTopPlayersByChatMessages(TOP_PLAYERS);
            case "loved":
                return plugin.getDatabaseManager().getTopPlayersByPositiveRep(TOP_PLAYERS);
            case "hated":
                return plugin.getDatabaseManager().getTopPlayersByNegativeRep(TOP_PLAYERS);
            default:
                return List.of();
        }
    }

    private void displayLeaderboard(CommandSender sender, String category, List<PlayerStats> players) {
        String categoryDisplay = getCategoryDisplayName(category);

        String headerText = "TOP " + categoryDisplay.toUpperCase();
        String dashes = "-".repeat(40);

        Component header = Component.text(dashes.substring(0, 8) + " ", NamedTextColor.GRAY)
            .append(Component.text(headerText, NamedTextColor.GOLD))
            .append(Component.text(" " + dashes.substring(0, 40 - headerText.length() - 9), NamedTextColor.GRAY));

        sender.sendMessage(header);

        // Player entries
        for (int i = 0; i < players.size(); i++) {
            PlayerStats stats = players.get(i);
            int rank = i + 1;

            String rankStr = String.format("%2d. ", rank);
            
            Component entry = Component.text(rankStr, NamedTextColor.WHITE)
                .append(PlayerUtil.getPlayerDisplayName(stats.getUsername(), stats.getUuid()))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(getValueComponent(category, stats));

            sender.sendMessage(entry);
        }

        // Footer
        Component footer = Component.text(dashes, NamedTextColor.GRAY);
        sender.sendMessage(footer);
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "timeplayed": return "Time Played";
            case "kills": return "Kills";
            case "deaths": return "Deaths";
            case "chatter": return "Chat Messages";
            case "loved": return "Highest Positive Reputation";
            case "hated": return "Highest Negative Reputation";
            default: return category;
        }
    }

    private Component getValueComponent(String category, PlayerStats stats) {
        switch (category) {
            case "timeplayed":
                return Component.text(stats.getFormattedTimePlayed(), NamedTextColor.GREEN);
            case "kills":
                String killText = stats.getKills() == 1 ? "kill" : "kills";
                return Component.text(stats.getKills() + " " + killText, NamedTextColor.RED);
            case "deaths":
                String deathText = stats.getDeaths() == 1 ? "death" : "deaths";
                return Component.text(stats.getDeaths() + " " + deathText, NamedTextColor.DARK_RED);
            case "chatter":
                String messageText = stats.getChatMessages() == 1 ? "message" : "messages";
                return Component.text(stats.getChatMessages() + " " + messageText, NamedTextColor.AQUA);
            case "loved":
            case "hated":
                int netRep = stats.getNetRep();
                NamedTextColor netColor = netRep > 0 ? NamedTextColor.GREEN : (netRep < 0 ? NamedTextColor.RED : NamedTextColor.WHITE);
                String netDisplay = netRep > 0 ? "+" + netRep : (netRep < 0 ? "-" + Math.abs(netRep) : "0");
                return Component.text(netDisplay, netColor)
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text("+" + stats.getPositiveRep(), NamedTextColor.GREEN))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text("-" + stats.getNegativeRep(), NamedTextColor.RED))
                    .append(Component.text(")", NamedTextColor.GRAY));
            default:
                return Component.text("Unknown", NamedTextColor.GRAY);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("timeplayed", "kills", "deaths", "chatter", "loved", "hated")
                .stream()
                .filter(category -> category.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
