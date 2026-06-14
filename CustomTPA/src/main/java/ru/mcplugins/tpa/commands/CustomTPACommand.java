package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;

public class CustomTPACommand implements CommandExecutor {
    private final TPAPlugin plugin;
    private final ConfigManager config;

    public CustomTPACommand(TPAPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            Component prefix = sender instanceof Player
                    ? config.getMessage((Player) sender, "prefix")
                    : Component.text("[TPA] ");
            sender.sendMessage(prefix.append(
                    Component.text("CustomTPA v" + plugin.getDescription().getVersion() + " by MrKronick")));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("customtpa.reload")) {
                sender.sendMessage(sender instanceof Player
                        ? config.getMessage((Player) sender, "no-permission")
                        : Component.text("Нет прав"));
                return true;
            }
            config.reload();
            sender.sendMessage(sender instanceof Player
                    ? config.getMessage((Player) sender, "config-reloaded")
                    : Component.text("Конфигурация перезагружена"));
            return true;
        }
        Component prefix = sender instanceof Player
                ? config.getMessage((Player) sender, "prefix")
                : Component.text("[TPA] ");
        sender.sendMessage(prefix.append(Component.text("Используйте: /ctpa reload")));
        return true;
    }
}