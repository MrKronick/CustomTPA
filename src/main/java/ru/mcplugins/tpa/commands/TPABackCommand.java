package ru.mcplugins.tpa.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.util.MessageUtil;

public class TPABackCommand implements CommandExecutor {

    private final TPAPlugin plugin;
    private final ConfigManager config;

    public TPABackCommand(TPAPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!config.isTpaBackEnabled()) {
            MessageUtil.send(player, config.getMessage(player, "tpa-back-disabled"));
            return true;
        }
        Location loc = plugin.consumeLastLocation(player);
        if (loc == null) {
            MessageUtil.send(player, config.getMessage(player, "tpa-back-none"));
            return true;
        }
        player.teleport(loc);
        MessageUtil.send(player, config.getMessage(player, "tpa-back-success"));
        return true;
    }
}