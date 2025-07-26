package com.jellypudding.offlineStats.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
}
