package ru.mcplugins.tpa.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.util.MessageUtil;

public class TPAForceCommand implements CommandExecutor {

    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPAForceCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("customtpa.force")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }
        if (!config.isForceTpaEnabled()) {
            MessageUtil.send(player, config.getMessage(player, "force-tpa-disabled"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(player, config.getMessage(player, "force-tpa-usage"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(player, config.getMessage(player, "player-offline"));
            return true;
        }
        if (target.equals(player)) {
            MessageUtil.send(player, config.getMessage(player, "self-request"));
            return true;
        }

        plugin.saveLastLocation(player);
        player.teleport(target);
        MessageUtil.send(player, config.getMessage(player, "force-tpa-success"));
        return true;
    }
}