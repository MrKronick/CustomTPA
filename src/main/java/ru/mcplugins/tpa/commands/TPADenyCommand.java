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
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.util.MessageUtil;

import java.util.List;
import java.util.stream.Collectors;

public class TPADenyCommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;
    private final StatsManager statsManager;

    public TPADenyCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config, StatsManager statsManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }
        if (!player.hasPermission("tpa.deny")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }
        List<RequestManager.TPARequest> incoming = requestManager.getIncomingRequests(player.getUniqueId());
        if (incoming.isEmpty()) {
            MessageUtil.send(player, config.getMessage(player, "no-requests"));
            return true;
        }
        RequestManager.TPARequest request;
        if (args.length > 0) {
            String name = args[0];
            request = incoming.stream()
                    .filter(r -> Bukkit.getOfflinePlayer(r.getSender()).getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (request == null) {
                MessageUtil.send(player, MiniMessage.miniMessage().deserialize("<red>Request from " + name + " not found.</red>"));
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
            MessageUtil.send(player, config.getMessage(player, "prefix").append(Component.text("Requests: ")).append(list));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer != null && senderPlayer.isOnline()) {
            MessageUtil.send(senderPlayer, config.format(senderPlayer, player, "request-denied"));
        }
        MessageUtil.send(player, config.format(player, senderPlayer, "request-denied"));
        requestManager.removeRequest(request.getSender(), request.getTarget());
        statsManager.addDenied(player.getUniqueId());
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