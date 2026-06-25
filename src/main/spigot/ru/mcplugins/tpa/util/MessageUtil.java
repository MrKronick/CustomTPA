package ru.mcplugins.tpa.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static final BungeeComponentSerializer serializer = BungeeComponentSerializer.get();

    public static void send(Player player, Component message) {
        BaseComponent[] components = serializer.serialize(message);
        player.spigot().sendMessage(components);
    }

    public static void sendActionBar(Player player, Component message) {
        BaseComponent[] components = serializer.serialize(message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
    }
}