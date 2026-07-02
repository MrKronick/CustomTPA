package ru.mcplugins.tpa.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class MessageUtil {
    public static void send(Player player, Component message) {
        player.sendMessage(message);
    }
    public static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }
}