package com.jellypudding.offlineStats.utils;

import com.jellypudding.offlineStats.OfflineStats;

import java.awt.Color;

public class DiscordUtil {

    private final OfflineStats plugin;

    public DiscordUtil(OfflineStats plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(String title, String message, Color color) {
        if (!plugin.isDiscordRelayEnabled()) {
            return;
        }

        try {
            Class<?> discordRelayAPI = Class.forName("com.jellypudding.discordRelay.DiscordRelayAPI");
            java.lang.reflect.Method isReady = discordRelayAPI.getMethod("isReady");
            java.lang.reflect.Method sendFormattedMessage = discordRelayAPI.getMethod("sendFormattedMessage", String.class, String.class, Color.class);

            if ((Boolean) isReady.invoke(null)) {
                sendFormattedMessage.invoke(null, title, message, color);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord announcement: " + e.getMessage());
        }
    }
}
