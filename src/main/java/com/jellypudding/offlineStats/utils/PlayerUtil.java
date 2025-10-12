package com.jellypudding.offlineStats.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class PlayerUtil {

    public static UUID getPlayerUUID(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer.getUniqueId();
            }
        } catch (Exception e) {
            // Player not found in Mojang API.
            return null;
        }

        return null;
    }

    public static String getExactPlayerName(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                String cachedName = offlinePlayer.getName();
                if (cachedName != null) {
                    return cachedName;
                }
            }
        } catch (Exception e) {
            // Player not found in Mojang API.
            return playerName;
        }

        return playerName;
    }

    public static Component getPlayerDisplayName(String playerName, UUID playerUuid) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.displayName();
        }

        Plugin chromaTagPlugin = Bukkit.getPluginManager().getPlugin("ChromaTag");
        if (chromaTagPlugin != null && chromaTagPlugin.isEnabled()) {
            try {
                Class<?> chromaTagClass = chromaTagPlugin.getClass();
                java.lang.reflect.Method getPlayerColourMethod = chromaTagClass.getMethod("getPlayerColor", UUID.class);
                TextColor colour = (TextColor) getPlayerColourMethod.invoke(chromaTagPlugin, playerUuid);

                if (colour != null) {
                    return Component.text(playerName, colour);
                }
            } catch (Exception e) {
            }
        }
        // Fallback.
        return Component.text(playerName, NamedTextColor.GOLD);
    }
}
