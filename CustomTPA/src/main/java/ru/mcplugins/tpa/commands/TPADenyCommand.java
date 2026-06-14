package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

import java.util.List;
import java.util.stream.Collectors;

public class TPADenyCommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPADenyCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!player.hasPermission("tpa.deny")) {
            player.sendMessage(config.getMessage(player, "no-permission"));
            return true;
        }
        List<RequestManager.TPARequest> incoming = requestManager.getIncomingRequests(player.getUniqueId());
        if (incoming.isEmpty()) {
            player.sendMessage(config.getMessage(player, "no-requests"));
            return true;
        }
        RequestManager.TPARequest request;
        if (args.length > 0) {
            String name = args[0];
            request = incoming.stream()
                    .filter(r -> Bukkit.getOfflinePlayer(r.getSender()).getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (request == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Запрос от " + name + " не найден.</red>"));
                return true;
            }
        } else {
            Component list = Component.join(
                net.kyori.adventure.text.JoinConfiguration.commas(true),
                incoming.stream()
                    .map(r -> {
                        String name = Bukkit.getOfflinePlayer(r.getSender()).getName();
                        return Component.text(name != null ? name : "Unknown")
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpadeny " + name));
                    })
                    .toArray(Component[]::new)
            );
            player.sendMessage(config.getMessage(player, "prefix").append(Component.text("Запросы: ")).append(list));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer != null && senderPlayer.isOnline()) {
            senderPlayer.sendMessage(config.format(senderPlayer, player, "request-denied"));
        }
        player.sendMessage(config.format(player, senderPlayer, "request-denied"));
        requestManager.removeRequest(request.getSender(), request.getTarget());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return null;
        if (args.length == 1) {
            return requestManager.getIncomingRequests(player.getUniqueId()).stream()
                    .map(r -> Bukkit.getOfflinePlayer(r.getSender()).getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}