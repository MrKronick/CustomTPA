package ru.mcplugins.tpa.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.util.MessageUtil;

public class TPAAutoCommand implements CommandExecutor {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPAAutoCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
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
        if (!player.hasPermission("tpa.auto")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }

        boolean current = requestManager.isAutoAccept(player.getUniqueId());
        requestManager.setAutoAccept(player.getUniqueId(), !current);

        if (!current) {
            MessageUtil.send(player, config.getMessage(player, "auto-accept-on"));
        } else {
            MessageUtil.send(player, config.getMessage(player, "auto-accept-off"));
        }
        return true;
    }
}