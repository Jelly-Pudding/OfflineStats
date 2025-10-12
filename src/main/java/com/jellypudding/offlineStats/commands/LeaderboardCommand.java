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
            sender.sendMessage(Component.text("Invalid category! Valid categories: timeplayed, kills, deaths, chatter", NamedTextColor.RED));
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
        return Arrays.asList("timeplayed", "kills", "deaths", "chatter").contains(category);
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
            default:
                return List.of();
        }
    }

    private void displayLeaderboard(CommandSender sender, String category, List<PlayerStats> players) {
        String categoryDisplay = getCategoryDisplayName(category);

        int maxNameLength = players.stream()
            .mapToInt(stats -> stats.getUsername().length())
            .max()
            .orElse(10);

        int nameColumnWidth = Math.max(maxNameLength + 2, 12);

        String headerText = "TOP " + categoryDisplay.toUpperCase();
        int totalWidth = Math.max(nameColumnWidth + 20, headerText.length() + 16);
        String dashes = "-".repeat(totalWidth);

        Component header = Component.text(dashes.substring(0, 8) + " ", NamedTextColor.GRAY)
            .append(Component.text(headerText, NamedTextColor.GOLD))
            .append(Component.text(" " + dashes.substring(0, totalWidth - headerText.length() - 9), NamedTextColor.GRAY));

        sender.sendMessage(header);

        // Player entries with proper alignment
        for (int i = 0; i < players.size(); i++) {
            PlayerStats stats = players.get(i);
            int rank = i + 1;

            Component rankComponent = Component.text(String.format("%2d.", rank), NamedTextColor.WHITE);
            Component playerName = PlayerUtil.getPlayerDisplayName(stats.getUsername(), stats.getUuid());
            Component value = getValueComponent(category, stats);

            int spacesNeeded = nameColumnWidth - stats.getUsername().length();
            String spacing = " ".repeat(Math.max(spacesNeeded, 1));

            Component entry = rankComponent
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(playerName)
                .append(Component.text(spacing, NamedTextColor.WHITE))
                .append(value);

            sender.sendMessage(entry);
        }

        // Footer with matching length
        Component footer = Component.text(dashes, NamedTextColor.GRAY);
        sender.sendMessage(footer);
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "timeplayed": return "Time Played";
            case "kills": return "Kills";
            case "deaths": return "Deaths";
            case "chatter": return "Chat Messages";
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
            default:
                return Component.text("Unknown", NamedTextColor.GRAY);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("timeplayed", "kills", "deaths", "chatter")
                .stream()
                .filter(category -> category.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
