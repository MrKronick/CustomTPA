package ru.mcplugins.tpa.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

public class TPACancelCommand implements CommandExecutor {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPACancelCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!player.hasPermission("tpa.cancel")) {
            player.sendMessage(config.getMessage(player, "no-permission"));
            return true;
        }
        RequestManager.TPARequest out = requestManager.getOutgoingRequest(player.getUniqueId());
        if (out == null) {
            player.sendMessage(config.getMessage(player, "no-outgoing"));
            return true;
        }
        Player target = Bukkit.getPlayer(out.getTarget());
        if (target != null && target.isOnline()) {
            target.sendMessage(config.format(target, player, "request-cancelled"));
        }
        player.sendMessage(config.format(player, target, "request-cancelled"));
        requestManager.removeRequest(player.getUniqueId(), out.getTarget());
        return true;
    }
}